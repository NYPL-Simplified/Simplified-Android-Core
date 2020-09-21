package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Option
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP.createAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.http.core.HTTPProblemReportLogging
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A minimal class around the various annotations and user profile REST calls.
 */

class ReaderBookmarkHTTPCalls(
  private val objectMapper: ObjectMapper,
  private val http: HTTPType
) : org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkHTTPCallsType {

  private val logger = LoggerFactory.getLogger(ReaderBookmarkHTTPCalls::class.java)

  override fun bookmarksGet(
    annotationsURI: URI,
    credentials: AccountAuthenticationCredentials
  ): List<BookmarkAnnotation> {
    val auth =
      createAuthenticatedHTTP(credentials)
    val result =
      this.http.get(Option.some(auth), annotationsURI, 0L)

    return result.match<List<BookmarkAnnotation>, IOException>(
      { error -> logAndFail(annotationsURI, error) },
      { exception -> throw exception.error },
      { success -> deserializeBookmarksFromStream(success.value) }
    )
  }

  override fun bookmarkDelete(
    bookmarkURI: URI,
    credentials: AccountAuthenticationCredentials
  ) {
    val auth =
      createAuthenticatedHTTP(credentials)
    val result =
      this.http.delete(Option.some(auth), bookmarkURI, "application/octet-stream")

    return result.match<Unit, IOException>(
      { error -> logAndFail(bookmarkURI, error) },
      { exception -> throw exception.error },
      { success -> deserializeBookmarksFromStream(success.value) }
    )
  }

  override fun bookmarkAdd(
    annotationsURI: URI,
    credentials: AccountAuthenticationCredentials,
    bookmark: BookmarkAnnotation
  ) {
    val data =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToBytes(this.objectMapper, bookmark)
    val auth =
      createAuthenticatedHTTP(credentials)
    val result =
      this.http.post(Option.some(auth), annotationsURI, data, "application/ld+json")

    return result.match<Unit, IOException>(
      { error -> logAndFail(annotationsURI, error) },
      { exception -> throw exception.error },
      { }
    )
  }

  override fun syncingEnable(
    settingsURI: URI,
    credentials: AccountAuthenticationCredentials,
    enabled: Boolean
  ) {
    val data =
      serializeSynchronizeEnableData(enabled)
    val auth =
      createAuthenticatedHTTP(credentials)
    val result =
      this.http.put(Option.some(auth), settingsURI, data, "vnd.librarysimplified/user-profile+json")

    return result.match<Unit, IOException>(
      { error -> logAndFail(settingsURI, error) },
      { exception -> throw exception.error },
      { }
    )
  }

  override fun syncingIsEnabled(
    settingsURI: URI,
    credentials: AccountAuthenticationCredentials
  ): Boolean {
    val auth =
      createAuthenticatedHTTP(credentials)
    val result =
      this.http.get(Option.some(auth), settingsURI, 0L)

    return result.match<Boolean, IOException>(
      { error -> logAndFail(settingsURI, error) },
      { exception -> throw exception.error },
      { success -> deserializeSyncingEnabledFromStream(success.value) }
    )
  }

  private fun <T> logAndFail(uri: URI, error: HTTPResultError<InputStream>): T {
    HTTPProblemReportLogging.logError(
      this.logger, uri, error.message, error.status, error.problemReport
    )
    throw IOException("$uri received ${error.status} ${error.message}")
  }

  private fun deserializeBookmarksFromStream(value: InputStream): List<BookmarkAnnotation> {
    val node =
      this.objectMapper.readTree(value)
    val response =
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationResponseFromJSON(node)
    return response.first.items
  }

  private fun deserializeSyncingEnabledFromStream(value: InputStream): Boolean {
    return deserializeSyncingEnabledFromJSON(this.objectMapper.readTree(value))
  }

  private fun deserializeSyncingEnabledFromJSON(node: JsonNode): Boolean {
    return deserializeSyncingEnabledFromJSONObject(JSONParserUtilities.checkObject(null, node))
  }

  private fun deserializeSyncingEnabledFromJSONObject(node: ObjectNode): Boolean {
    val settings = JSONParserUtilities.getObject(node, "settings")

    return if (settings.has("simplified:synchronize_annotations")) {
      val text: String? = settings.get("simplified:synchronize_annotations").asText()
      text == "true"
    } else {
      false
    }
  }

  private fun serializeSynchronizeEnableData(enabled: Boolean): ByteArray {
    val settingsNode = this.objectMapper.createObjectNode()
    settingsNode.put("simplified:synchronize_annotations", enabled)
    val node = this.objectMapper.createObjectNode()
    node.put("settings", settingsNode)
    return this.objectMapper.writeValueAsBytes(node)
  }
}
