package org.nypl.simplified.app

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import com.android.volley.*
import com.android.volley.Response.Listener
import com.android.volley.toolbox.Volley
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.io7m.jfunctional.Some
import org.json.JSONObject
import org.joda.time.Instant
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.simplified.app.reader.ReaderBookLocation
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountsControllerType
import org.nypl.simplified.books.core.AnnotationResponse
import org.nypl.simplified.books.core.BookmarkAnnotation
import org.nypl.simplified.multilibrary.Account
import org.nypl.simplified.volley.NYPLJsonObjectRequest
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Performs work relevant to syncing OPDS Annotations.
 * Current features for books:
 * 1 - Current reading position
 * 2 - Bookmarks saved by the user
 */
class AnnotationsManager(private val libraryAccount: Account,
                         private val credentials: AccountCredentials,
                         private val context: Context)
{
  private val requestQueue = Volley.newRequestQueue(context)

  private var syncPermissionStatusRequestPending = false
  private var syncPermissionStatusCompletions = mutableListOf<(Boolean,Boolean)->Unit>()

  private companion object {
    val LOG = LoggerFactory.getLogger(AnnotationsManager::class.java)!!
    const val SyncPermissionStatusTimeout = 60L
    const val ReadingPositionTimeout = 30L
    const val BookmarksRequestTimeout = 30L
    const val BookmarkDeleteTimeout = 20L
  }

  /**
   * Queries the server, and based on a set of conditions, decides whether
   * or not to inform the user about "Syncing" in an Alert Dialog, where
   * the user can choose to dismiss/ignore, or turn it on for the first time.
   * @param account The particular library for the permission status request.
   * @param completion Asynchronous handler for the user's decision, or error.
   */
  fun requestServerSyncPermissionStatus(account: AccountsControllerType,
                                        completion: (enableSync: Boolean) -> Unit)
  {
    if (!syncIsPossible(account)) {
      LOG.debug("Account does not satisfy conditions for sync setting request.")
      completion(false)
      return
    }

    if (Simplified.getSharedPrefs().getBoolean("userHasSeenFirstTimeSyncMessage") == true &&
        Simplified.getSharedPrefs().getBoolean("syncPermissionGranted", libraryAccount.id) == false) {
      completion(false)
      return
    }

    syncPermissionStatusUriRequest { initialized, syncIsPermitted ->
      if (initialized && syncIsPermitted) {
        Simplified.getSharedPrefs().putBoolean("userHasSeenFirstTimeSyncMessage", true)
        LOG.debug("Sync has already been enabled on the server. Enable here as well.")
        completion(true)
      } else if (!initialized && !Simplified.getSharedPrefs().getBoolean("userHasSeenFirstTimeSyncMessage")) {
        LOG.debug("Sync has never been initialized for the patron. Proceeding with opt-in alert.")
        presentFirstTimeSyncAlertDialog(completion)
      } else {
        LOG.debug("Sync has been initialized and not permitted. Continuing with sync disabled.")
        completion(false)
      }
    }
  }

  private fun syncPermissionStatusUriRequest(completion: (initialized: Boolean, syncIsPermitted: Boolean) -> Unit)
  {
    syncPermissionStatusCompletions.add(completion)

    if (syncPermissionStatusRequestPending) {
      LOG.debug("Request already waiting. Adding response to same result of pending request.")
      return
    }
    syncPermissionStatusRequestPending = true

    val baseCatalogUri = libraryAccount.catalogUrl?.let { Uri.parse(it) }
    val permissionRequestUri = if (baseCatalogUri != null) {
      Uri.withAppendedPath(baseCatalogUri, "patrons/me/")
    } else {
      LOG.debug("Could not create Annotations URL from Main Feed URL." +
          "Abandoning attempt to retrieve sync setting.")
      completion(true, false)
      return
    }

    val request = NYPLJsonObjectRequest(
        Request.Method.GET,
        permissionRequestUri.toString(),
        credentials.barcode.toString(),
        credentials.pin.toString(),
        null,
        Listener<JSONObject> { response ->
          syncPermissionStatusCompletions.forEach { queuedCompletion ->
            handleSyncPermission(response, queuedCompletion)
          }
          syncPermissionStatusCompletions.clear()
          syncPermissionStatusRequestPending = false
        },
        Response.ErrorListener { error ->
          LOG.error("GET request fail! Error: ${error.message}")
          syncPermissionStatusCompletions.clear()
          syncPermissionStatusRequestPending = false
          completion(true, false)
        })

    request.retryPolicy = DefaultRetryPolicy(
        TimeUnit.SECONDS.toMillis(SyncPermissionStatusTimeout).toInt(),
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

    requestQueue.add(request)
  }

  private fun handleSyncPermission(response: JSONObject,
                                   completion: (initialized: Boolean, syncIsPermitted: Boolean) -> Unit)
  {
    UIThread.checkIsUIThread()
    try {
      val settings = response.getJSONObject("settings")
      val syncSettingsBool = settings.getBoolean("simplified:synchronize_annotations")
      completion(true, syncSettingsBool)
    } catch (e: java.lang.Exception) {
      LOG.error("Exception thrown during JSON deserialization: ${e.localizedMessage}")
      completion(false, false)
    }
  }

  /**
   * Set the current user's permission setting on the server. If enabled is set to 'false',
   * all annotations for the current user will be deleted as part of this request.
   */
  fun updateServerSyncPermissionStatus(enabled: Boolean,
                                       completion: (successful: Boolean) -> Unit)
  {
    val catalogUriString = libraryAccount.catalogUrl ?: null
    val baseUri = if (catalogUriString != null) Uri.parse(catalogUriString) else null
    val patronsUpdateUri = if (baseUri != null) Uri.withAppendedPath(baseUri, "patrons/me/") else null

    if (patronsUpdateUri == null) {
      LOG.debug("Could not get Annotations URI. Abandoning attempt to update sync setting.")
      completion(false)
      return
    }

    val parameters = mapOf<String,Any>("settings" to mapOf("simplified:synchronize_annotations" to enabled))

    setSyncPermissionUriRequest(patronsUpdateUri.toString(), parameters, 20) { isSuccessful ->
      if (!isSuccessful) {
        presentAlertForSyncSettingError()
      }
      completion(isSuccessful)
    }
  }

  private fun setSyncPermissionUriRequest(uri: String,
                                          parameters: Map<String, Any>,
                                          timeout: Long?,
                                          completion: (success: Boolean) -> Unit)
  {
    val jsonBody = JSONObject(parameters)
    val additionalHeaders = mapOf("Content-Type" to "vnd.librarysimplified/user-profile+json")

    val request = NYPLJsonObjectRequest(
        Request.Method.PUT,
        uri,
        credentials.barcode.toString(),
        credentials.pin.toString(),
        jsonBody,
        additionalHeaders,
        Listener<JSONObject> { _ ->
          completion(true)
        },
        Response.ErrorListener { error ->
          LOG.debug("PUT request fail! Error: ${error.message}")
        })

    if (timeout != null) {
      request.retryPolicy = DefaultRetryPolicy(
          TimeUnit.SECONDS.toMillis(timeout).toInt(),
          DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
          DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
    }

    requestQueue.add(request)
  }

  /**
   * Get the current reading position for the given book and current user.
   * @param bookID Identifier for the entry
   * @param uri The Annotation ID, URI, for the OPDS Entry
   * @param completion Called asynchronously by the network request to return the CFI
   */
  fun requestReadingPositionOnServer(bookID: String?, uri: String?,
                                     completion: (location: ReaderBookLocation?) -> Unit ) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }
    if (uri == null || bookID == null) {
      LOG.debug("Required parameters are unexpectedly null")
      return
    }

    val request = NYPLJsonObjectRequest(
        Request.Method.GET,
        uri,
        credentials.barcode.toString(),
        credentials.pin.toString(),
        null,
        Listener<JSONObject> { response ->
          completion(bookLocationFromString(response))
        },
        Response.ErrorListener { error ->
          LOG.debug("GET request fail! Error: ${error.message}")
          completion(null)
        })

    request.retryPolicy = DefaultRetryPolicy(
        TimeUnit.SECONDS.toMillis(ReadingPositionTimeout).toInt(),
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

    requestQueue.add(request)
  }

  private fun bookLocationFromString(JSON: JSONObject): ReaderBookLocation?
  {
    val mapper = jacksonObjectMapper()
    val annotationResponse: AnnotationResponse? = mapper.readValue(JSON.toString())

    if (annotationResponse == null) {
      LOG.error("Annotation response did not correctly deserialize.")
      return null
    }

    annotationResponse.first.items.forEach {
      if (it.motivation == "http://librarysimplified.org/terms/annotation/idling") {
        val value = it.target.selector.value
        val valueJson = JSONObject(value)
        return ReaderBookLocation.fromJSON(valueJson)
      }
    }
    return null
  }

  /**
   * Update the current reading position for the given book and current user.
   * @param bookID Identifier for the entry
   * @param locationJson JSON string that represents the book position stored on annotation server,
   * provided and parsed upstream by Readium.
   */
  fun updateReadingPosition(bookID: String, locationJson: String)
  {
    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }

    val deviceIDString = if (credentials.adobeDeviceID.isSome) {
      (credentials.adobeDeviceID as Some<AdobeDeviceID>).get().value
    } else {
      LOG.error("Adobe Device ID was null. No device set in body for annotation.")
      "null"
    }

    val bodyObject = mapOf(
        "@context" to "http://www.w3.org/ns/anno.jsonld",
        "type" to "Annotation",
        "motivation" to "http://librarysimplified.org/terms/annotation/idling",
        "target" to mapOf(
            "source" to bookID,
            "selector" to mapOf(
                "type" to "oa:FragmentSelector",
                "value" to locationJson
            )
        ),
        "body" to mapOf(
            "http://librarysimplified.org/terms/time" to Instant().toString(),
            "http://librarysimplified.org/terms/device" to deviceIDString
        )
    )

    try {
      val mapper = ObjectMapper()
      val jsonBodyString= mapper.writer().writeValueAsString(bodyObject)

      postAnnotation(jsonBodyString, 20, { isSuccessful, response ->
        if (isSuccessful) {
          LOG.debug("Success: Marked Reading Position To Server. Response: $response")
        } else {
          LOG.error("Annotation not posted.")
        }
      })

    } catch (e: IOException) {
      LOG.error("Error serializing JSON from Map Object")
      return
    }
  }

  private fun postAnnotation(bodyParameters: String, timeout: Long?,
                             completion: (isSuccessful: Boolean, annotationID: String?) -> Unit)
  {
    val catalogUriString = libraryAccount.catalogUrl
    val baseUri = if (catalogUriString != null) Uri.parse(catalogUriString) else null
    val mainFeedAnnotationUri = if (baseUri != null) Uri.withAppendedPath(baseUri, "annotations/") else null
    val requestUri = mainFeedAnnotationUri?.toString()
    if (requestUri == null) {
      LOG.error("No Catalog Annotation Uri present for POST request.")
      return
    }

    try {
      val jsonObjectBody = JSONObject(bodyParameters)
      LOG.debug(jsonObjectBody.toString())
      val request = NYPLJsonObjectRequest(
          Request.Method.POST,
          requestUri,
          credentials.barcode.toString(),
          credentials.pin.toString(),
          jsonObjectBody,
          null,
          Listener<JSONObject> { response ->
            LOG.debug("Annotation POST: Success 200.")
            val serverAnnotationID = response.getString("id")
            completion(true, serverAnnotationID)
          },
          Response.ErrorListener { error ->
            LOG.error("POST request fail! Network Error Cause: ${error.cause ?: "error cause was null"}")
            completion(false, null)
          })

      if (timeout != null) {
        request.retryPolicy = DefaultRetryPolicy(
            TimeUnit.SECONDS.toMillis(timeout).toInt(),
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
      }

      requestQueue.add(request)

    } catch (e: java.lang.Exception) {
      LOG.error("Post Annotation: Error creating JSONObject from Kotlin Map Type.")
    }
  }

  /**
   * Get a list of any bookmark annotations created for the given book and the current user.
   * @param uri The Annotation ID/URI for the OPDS Entry
   * @param completion Called asynchronously by the network request to return the bookmarks
   */
  fun requestBookmarksFromServer(uri: String,
                                 completion: (bookmarks: List<BookmarkAnnotation>?) -> Unit) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      completion(null)
      return
    }

    val request = NYPLJsonObjectRequest(
        Request.Method.GET,
        uri,
        credentials.barcode.toString(),
        credentials.pin.toString(),
        null,
        Listener<JSONObject> { response ->

          val json = response.toString()

          val mapper = jacksonObjectMapper()
          var annotationResponse: AnnotationResponse?
          try {
            annotationResponse = mapper.readValue(json)
          } catch (e: JsonMappingException) {
            LOG.error("Cancelling download request. JsonMappingException for annotations:\n $e")
            completion(null)
            return@Listener
          }

          val bookmarks = annotationResponse.first.items.filter {
            it.motivation.contains("bookmarking", true)
          }
          LOG.debug("Bookmarks downloaded from server:\n$bookmarks")
          completion(bookmarks)
        },
        Response.ErrorListener { error ->
          LOG.error("GET request fail! Error: ${error.message}")
          completion(null)
        })

    request.retryPolicy = DefaultRetryPolicy(
        TimeUnit.SECONDS.toMillis(BookmarksRequestTimeout).toInt(),
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    )

    requestQueue.add(request)
  }

  /**
   * Post a new bookmark to the server for the current user for a particular entry.
   * @param bookmark the annotation object to post to the server
   * @param completion Returns the new UUID/URI created for the annotation
   */
  fun postBookmarkToServer(bookmark: BookmarkAnnotation,
                           completion: (serverID: String?) -> Unit) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      completion(null)
      return
    }

    try {
      val jsonBody = bookmark.toString()
      postAnnotation(jsonBody, 20) { _, serverID ->
        completion(serverID)
      }
    } catch (e: Exception) {
      LOG.error("Error serializing JSON from Kotlin object")
      return
    }
  }

  /**
   * Delete a bookmark on the server.
   */
  fun deleteBookmarkOnServer(annotationID: String,
                             completion: (isSuccessful: Boolean) -> Unit) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      completion(false)
      return
    }

    val request = NYPLJsonObjectRequest(
        Request.Method.DELETE,
        annotationID,
        credentials.barcode.toString(),
        credentials.pin.toString(),
        null,
        Listener<JSONObject> { response ->
          completion(true)
        },
        Response.ErrorListener { error ->
          completion(false)
        })

    request.retryPolicy = DefaultRetryPolicy(
        TimeUnit.SECONDS.toMillis(BookmarkDeleteTimeout).toInt(),
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    )

    requestQueue.add(request)
  }

  /**
   * Sync is possible if a user is logged in and the current active library
   * has SimplyE sync support enabled.
   */
  private fun syncIsPossible(userAccount: AccountsControllerType): Boolean {
    return userAccount.accountIsLoggedIn() && libraryAccount.supportsSimplyESync()
  }

  /**
   * Sync is permitted if the user has explicitly enabled the feature on this device,
   * or inherited the permission from a user's other device.
   */
  fun syncIsPossibleAndPermitted(): Boolean {
    return try {
      val app: SimplifiedCatalogAppServicesType? = Simplified.getCatalogAppServices()
      val currentAccount = app?.books as? AccountsControllerType
      val syncIsPossible = if (currentAccount != null) syncIsPossible(currentAccount) else false
      val libraryID = Simplified.getCurrentAccount()
      val syncPermissionGranted = Simplified.getSharedPrefs().getBoolean("syncPermissionGranted", libraryID.id)
      syncIsPossible && syncPermissionGranted
    } catch (e: Exception) {
      LOG.error("Exception thrown accessing SimplifiedCatalogAppServicesType to obtain sync status.")
      false
    }
  }

  private fun presentFirstTimeSyncAlertDialog(completion: (enableSync: Boolean) -> Unit)
  {
    val builder= AlertDialog.Builder(context)
    with(builder) {
      setTitle(context.getString(R.string.firstTimeSyncAlertTitle))
      setMessage(context.getString(R.string.firstTimeSyncAlertMessage))
      builder.setNegativeButton(context.getString(R.string.firstTimeSyncAlertNegButton)) { _, _ ->
        Simplified.getSharedPrefs().putBoolean("userHasSeenFirstTimeSyncMessage", true)
        completion(false)
      }
      builder.setPositiveButton(context.getString(R.string.firstTimeSyncAlertPosButton)) { _, _ ->
        updateServerSyncPermissionStatus(true) { success ->
          Simplified.getSharedPrefs().putBoolean("userHasSeenFirstTimeSyncMessage", true)
          completion(success)
        }
      }

      builder.create()
      builder.show()
    }
  }

  private fun presentAlertForSyncSettingError()
  {
    val builder= AlertDialog.Builder(context)
    with(builder) {
      setTitle(context.getString(R.string.syncSettingAlertTitle))
      setMessage(R.string.syncSettingAlertMessage)
      setPositiveButton("OK", null)
      create()
      show()
    }
  }
}
