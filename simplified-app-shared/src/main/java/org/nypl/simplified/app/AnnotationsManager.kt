package org.nypl.simplified.app

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import com.android.volley.*
import com.android.volley.Response.Listener
import com.android.volley.toolbox.Volley
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.io7m.jfunctional.Some
import org.json.JSONException
import org.json.JSONObject
import org.joda.time.Instant
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.simplified.app.catalog.annotation.AnnotationResult
import org.nypl.simplified.app.reader.ReaderBookLocation
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountsControllerType
import org.nypl.simplified.multilibrary.Account
import org.nypl.simplified.volley.NYPLJsonObjectRequest
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Handles any network requests relevant to OPDS Annotations
 * for user settings or specific OPDS Entries for that user.
 */

class AnnotationsManager(private val libraryAccount: Account,
                         private val credentials: AccountCredentials,
                         private val context: Context) {

  private val requestQueue = Volley.newRequestQueue(context)

  private companion object {
    val LOG = LoggerFactory.getLogger(AnnotationsManager::class.java)!!
  }

  /**
   * Queries the server, and based on a set of conditions, decides whether
   * or not to inform the user about "Syncing" in an Alert Dialog, where
   * the user can choose to dismiss/ignore, or turn it on for the first time.
   * @param account The particular library for the permission status request.
   * @param completion Asynchronous handler for the user's decision, or error.
   */
  fun requestServerSyncPermissionStatus(account: AccountsControllerType,
                                        completion: (enableSync: Boolean) -> Unit) {

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

  private fun syncPermissionStatusUriRequest(completion: (initialized: Boolean, syncIsPermitted: Boolean) -> Unit) {

    val catalogUriString = libraryAccount.catalogUrl ?: null
    val baseUri = if (catalogUriString != null) Uri.parse(catalogUriString) else null
    val permissionRequestUri = if (baseUri != null) Uri.withAppendedPath(baseUri, "patrons/me/") else null

    if (permissionRequestUri == null) {
      LOG.debug("Could not create Annotations URL from Main Feed URL. Abandoning attempt to retrieve sync setting.")
      return
    }

    val request = NYPLJsonObjectRequest(
        Request.Method.GET,
        permissionRequestUri.toString(),
        credentials.barcode.toString(),
        credentials.pin.toString(),
        null,
        Listener<JSONObject> { response ->
          handleSyncPermission(response, completion)
        },
        Response.ErrorListener { error ->
          LOG.error("GET request fail! Error: ${error.message}")
        })

    request.retryPolicy = DefaultRetryPolicy(
        60 * 1000,
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

    requestQueue.add(request)
  }

  private fun handleSyncPermission(response: JSONObject,
                                   completion: (initialized: Boolean, syncIsPermitted: Boolean) -> Unit) {

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
                                       completion: (successful: Boolean) -> Unit) {

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
                                          timeout: Int?,
                                          completion: (success: Boolean) -> Unit) {

    val jsonBody = JSONObject(parameters)
    val additionalHeaders = mapOf<String,String>("Content-Type" to "vnd.librarysimplified/user-profile+json")

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
          timeout * 1000,
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

    //TODO Test to make sure passing null for body parameter works as expected

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
        30 * 1000,
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

    requestQueue.add(request)
  }

  private fun bookLocationFromString(JSON: JSONObject): ReaderBookLocation? {

    //TODO test this
    try {
      val serializedResult = Gson().fromJson(JSON.toString(), AnnotationResult::class.java)
      for (annotation in serializedResult.first.items) {
        if (annotation.motivation == "http://librarysimplified.org/terms/annotation/idling") {
          val value = annotation.target.selector.value
          val jsonObject = JSONObject(value)
          return ReaderBookLocation.fromJSON(jsonObject)
        }
      }
    } catch (e: java.lang.Exception) {
      LOG.error("Could not get or deserialize book location from server/JSON.")
    }
    return null
  }

  private fun handleSyncReadingPosition2(bookID: String,
                                         response: JSONObject?,
                                         completion: (jumpToLocation: ReaderBookLocation) -> Unit) {
    if (response == null) {
      return
    }
    LOG.info("response json object: \n${response.toString()}")

    try {
      val serializedResult = Gson().fromJson(response.toString(), AnnotationResult::class.java)
      for (annotation in serializedResult.first.items) {
        if (annotation.motivation == "http://librarysimplified.org/terms/annotation/idling") {
          val value = annotation.target.selector.value
          val jsonObject = JSONObject(value)
          val readerBookmark = ReaderBookLocation.fromJSON(jsonObject)
        }
      }
    } catch (e: JsonSyntaxException) {
      LOG.error("Json Syntax Exception. Skipping reading position sync.")
    } catch (e: JSONException) {

    }
  }

  private fun handleSyncReadingPosition(bookID: String,
                                        response: JSONObject,
                                        completion: (response: Map<String,String>?) -> Unit) {

    try {

      val first = response.getJSONObject("first")
      val items = first?.getJSONArray("items")
      if (items == null) {
        LOG.debug("Missing required key from Annotations response, or no items exist.")
        completion(null)
        return
      }

      for (i in 0 until items.length()) {

        val item = items.getJSONObject(i)
        val target = item?.getJSONObject("target")
        val source = target?.getString("source")
        val motivation = item?.getString("motivation")

        if (source == null || motivation == null) {
          completion(null)
          continue
        }

        if (source == bookID && motivation == "http://librarysimplified.org/terms/annotation/idling") {

          val selector = target.getJSONObject("selector")
          val serverCFI = selector?.getString("value")
          if (serverCFI == null) {
            LOG.error("No CFI saved for title on the server.")
            completion(null)
            return
          }

          val jsonObject = JSONObject(serverCFI)
          val readerBookmark = ReaderBookLocation.fromJSON(jsonObject)


          val readingPosAnnotation = mutableMapOf("serverCFI" to serverCFI)
          val body = item.getJSONObject("body")
          val device = body?.getString("http://librarysimplified.org/terms/device")
          val time = body?.getString("http://librarysimplified.org/terms/time")
          if (device != null) readingPosAnnotation["device"] = device
          if (time != null) readingPosAnnotation["time"] = time

          completion(readingPosAnnotation)
          return
        }
      }
      LOG.error("No annotation for reading position found for this book.")
      completion(null)
      return
    } catch (e: java.lang.Exception) {
      LOG.error("Exception thrown while parsing JSON with Volley")
      completion(null)
      return
    }
  }

  /**
   * Update the current reading position for the given book and current user.
   * @param bookID Identifier for the entry
   * @param uri The Annotation ID/URI for the OPDS Entry.
   * @param cfi JSON string that represents the book position,
   * provided and parsed upstream by Readium.
   */
  fun updateReadingPosition(bookID: String, cfi: String) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }

    val catalogUriString = libraryAccount.catalogUrl
    val baseUri = if (catalogUriString != null) Uri.parse(catalogUriString) else null
    val mainFeedAnnotationUri = if (baseUri != null) Uri.withAppendedPath(baseUri, "annotations/") else null
    val requestUri = mainFeedAnnotationUri?.toString()
    if (requestUri == null) {
      LOG.error("No Catalog Annotation Uri present for POST request.")
      return
    }

    val deviceIDString = if (credentials.adobeDeviceID.isSome) {
      (credentials.adobeDeviceID as Some<AdobeDeviceID>).get().value
    } else {
      null
    }

    val bodyObject = mapOf(
        "@context" to "http://www.w3.org/ns/anno.jsonld",
        "type" to "Annotation",
        "motivation" to "http://librarysimplified.org/terms/annotation/idling",
        "target" to mapOf(
            "source" to bookID,
            "selector" to mapOf(
                "type" to "oa:FragmentSelector",
                "value" to cfi
            )
        ),
        "body" to mapOf(
            "http://librarysimplified.org/terms/time" to Instant().toString(),
            "http://librarysimplified.org/terms/device" to deviceIDString
        )
    )

    try {
      val mapper = ObjectMapper()
      val jsonBodyString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bodyObject)

      postAnnotation(requestUri, jsonBodyString, null, { isSuccessful, _ ->
        if (isSuccessful) {
          //TODO is there any reason that we need to save and update the annotation ID created for a reading-position upload?
          LOG.debug("Success: Marked Reading Position To Server: $cfi")
        } else {
          LOG.error("Annotation not posted.")
        }
      })

    } catch (e: IOException) {
      LOG.error("Error serializing JSON from Map Object")
      return
    }
  }

  private fun postAnnotation(uri: String, bodyParameters: String, timeout: Int?,
                             completion: (isSuccessful: Boolean, annotationID: String?) -> Unit) {

    try {
      val jsonObjectBody = JSONObject(bodyParameters)
      val request = NYPLJsonObjectRequest(
          Request.Method.POST,
          uri,
          credentials.barcode.toString(),
          credentials.pin.toString(),
          jsonObjectBody,
          null,
          Listener<JSONObject> { response ->
            LOG.debug("Annotation POST: Success 200.")
            val serverAnnotationID = response.getString("id") as? String
            completion(true, serverAnnotationID)
          },
          Response.ErrorListener { error ->
            LOG.error("POST request fail! Network Response: ${error.networkResponse.toString()}")
            completion(false, null)
          })

      if (timeout != null) {
        request.retryPolicy = DefaultRetryPolicy(
            timeout * 1000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
      }

      requestQueue.add(request)

    } catch (e: java.lang.Exception) {
      LOG.error("Post Annotation: Error creating JSONObject from Kotlin Map Type.")
    }
  }


  //TODO Re-familiarize myself with the Class to represent Bookmarks/Page Positions on android...
  //TODO Do I not need to parse for the bookmark type? Is that done from the caller or sync manager on iOS?
  /**
   * Get a list of any bookmark-type annotations created for the given book and the current user.
   * @param bookID Identifier for the entry
   * @param uri The Annotation ID/URI for the OPDS Entry
   * @param completion Called asynchronously by the network request to return the bookmarks
   */
  fun requestBookmarksFromServer(bookID: String?, uri: Uri?,
                                 completion: (bookmarks: Any?) -> Unit) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      completion(null)
      return
    }
    if (bookID == null || uri == null) {
      LOG.error("Required parameter was unexpectedly null")
      return
    }

    NYPLJsonObjectRequest(
        Request.Method.GET,
        uri.toString(),
        credentials.barcode.toString(),
        credentials.pin.toString(),
        null,
        Listener<JSONObject> { response ->

          try {
            val first = response.getJSONObject("first")
            val items = first?.getJSONArray("items")
            if (items == null) {
              LOG.error("")  //TODO
              return@Listener
            }
            val bookmarks = mutableListOf<Any>()
            for (i in 0 until items.length()) {
              val item = items.getJSONObject(i)
              val bookmark = createBookmark(bookID, item)
              if (bookmark != null) {
                bookmarks.add(bookmark)
              } else {
                LOG.error("Could not create bookmark element from item.")
                continue
              }
            }
            completion(bookmarks)
          } catch (e: java.lang.Exception) {
            LOG.error("GET request fail! Error: ${e.message}")
          }

        },
        Response.ErrorListener { error ->
          LOG.error("GET request fail! Error: ${error.message}")
          completion(null)
        })
  }

  //TODO this class was essentially prepping the data to send to the Bookmark class constructor..
  private fun createBookmark(bookID: String, annotation: JSONObject): Any? {

    //TODO Cannot convert this until I see how bookmarks are represented on Android.
    return null
  }

  /**
   * Post a new bookmark to the server for the current user for a particular entry.
   * @param bookID Identifier for the entry
   * @param uri The Annotation ID/URI for the OPDS Entry
   * @param completion Returns the new UUID/URI created for the annotation
   */
  fun postBookmarkToServer(bookID: String, uri: Uri, bookmark: Any,
                           completion: (serverID: String?) -> Unit) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      completion(null)
      return
    }

    val catalogUriString = libraryAccount.catalogUrl ?: null
    val baseUri = if (catalogUriString != null) Uri.parse(catalogUriString) else null
    val mainFeedAnnotationUri = if (baseUri != null) Uri.withAppendedPath(baseUri, "annotations/") else null
    if (mainFeedAnnotationUri == null) {
      LOG.error("Required parameter was nil.")
      completion(null)
      return
    }

    //TODO fill in placeholder values

    val parametersObject = mapOf(
        "test" to "test"
    )

//    val parametersObject = mapOf(
//        "@context" to "http://www.w3.org/ns/anno.jsonld",
//        "type" to "Annotation",
//        "motivation" to "http://www.w3.org/ns/oa#bookmarking",
//        "target" to mapOf(
//            "source" to bookID,
//            "selector" to mapOf(
//                "type" to "oa:FragmentSelector",
//                "value" to bookmark.contentCfi
//            )
//        ),
//        "body" to mapOf(
//            "http://librarysimplified.org/terms/time" to bookmark.time,
//            "http://librarysimplified.org/terms/device" to bookmark.device ?: "",
//            "http://librarysimplified.org/terms/chapter" to bookmark.chapter ?: "",
//            "http://librarysimplified.org/terms/progressWithinChapter" to bookmark.progressWithinChapter,
//            "http://librarysimplified.org/terms/progressWithinBook" to bookmark.progressWithinBook
//        )
//    )

    try {
      val mapper = ObjectMapper()
      val jsonBodyString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parametersObject)

      postAnnotation(mainFeedAnnotationUri.toString(), jsonBodyString, null) { _, id ->
        completion(id)
      }

    } catch (e: IOException) {
      LOG.error("Error serializing JSON from Map Object")
      return
    }
  }

  /**
   * Delete a bookmark on the server.
   */
  fun deleteBookmarkFromServer(annotationID: String,
                               completion: (isSuccessful: Boolean) -> Unit) {

    if (!syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      completion(false)
      return
    }

    //TODO set timeout of 20 sec.

    NYPLJsonObjectRequest(
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

  }

  //TODO update type information for Bookmark when that's resolved
  //TODO decide is this class is actually required on Android
//  fun deleteBookmarks(bookmarks: List<Any>) {
//
//    if (!syncIsPossibleAndPermitted()) {
//      LOG.debug("Account does not support sync or sync is disabled.")
//      return
//    }
//
//    for (bookmark in bookmarks) {
//
//      if (bookmark.annotationID as? String == null) {   //TODO update for correct type
//        return
//      }
//      deleteBookmark(bookmark.annotationID) { isSuccessful ->
//        if (isSuccessful) {
//          LOG.debug("Server bookmark deleted: ${bookmark.annotationID}")
//        } else {
//          LOG.error("Bookmark not deleted from server. Moving on: ${bookmark.annotationID}")
//        }
//      }
//    }
//  }

  //TODO decide is this class is actually required
  fun uploadLocalBookmarks(bookmarks: List<Any>, bookID: String,
                           completion: (successful: List<Any>, failed: List<Any>) -> Unit) {

    //TODO STUB
  }


  /**
   * Sync is possible if a user is logged in and the current active library
   * has SimplyE sync support enabled.
   */
  //TODO can be private?
  fun syncIsPossible(userAccount: AccountsControllerType): Boolean {
    return userAccount.accountIsLoggedIn() && libraryAccount.supportsSimplyESync()
  }

  /**
   * Sync is permitted if the user has explicitly enabled the feature on this device,
   * or inherited activation from another device.
   */
  fun syncIsPossibleAndPermitted(): Boolean {
    try {
      val app: SimplifiedCatalogAppServicesType? = Simplified.getCatalogAppServices()
      val currentAccount = app?.books as? AccountsControllerType
      val syncIsPossible = if (currentAccount != null) syncIsPossible(currentAccount) else false
      val libraryID = Simplified.getCurrentAccount()
      val syncPermissionGranted = Simplified.getSharedPrefs().getBoolean("syncPermissionGranted", libraryID.id)
      return syncIsPossible && syncPermissionGranted
    } catch (e: Exception) {
      LOG.error("Exception thrown accessing SimplifiedCatalogAppServicesType to obtain sync status.")
      return false
    }
  }

  private fun presentFirstTimeSyncAlertDialog(completion: (enableSync: Boolean) -> Unit) {

    val builder= AlertDialog.Builder(context)
    builder.setTitle("SimplyE Sync")
    builder.setMessage("Enable sync to save your reading position and bookmarks to your other devices." +
        "\n\nYou can change this any time in Settings.")

    builder.setNegativeButton("Not Now") { dialog, which ->
      Simplified.getSharedPrefs().putBoolean("userHasSeenFirstTimeSyncMessage", true)
      completion(false)
    }
    builder.setPositiveButton("Enable Sync") { dialog, which ->
      updateServerSyncPermissionStatus(true) { success ->
        Simplified.getSharedPrefs().putBoolean("userHasSeenFirstTimeSyncMessage", true)
        completion(success)
      }
    }

    builder.create()
    builder.show()
  }

  private fun presentAlertForSyncSettingError() {

    val builder= AlertDialog.Builder(context)
    builder.setTitle("Error Changing Sync Setting")
    builder.setMessage("There was a problem contacting the server." +
        "\nPlease make sure you are connected to the internet, or try again later.")
    builder.setPositiveButton("OK", null)
    builder.create()
    builder.show()
  }
}
