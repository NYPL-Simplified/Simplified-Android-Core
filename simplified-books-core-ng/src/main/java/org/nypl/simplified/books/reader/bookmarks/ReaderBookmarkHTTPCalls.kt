package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Option
import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.http.core.HTTPProblemReportLogging
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.json.core.JSONParserUtilities
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A minimal class around the various annotations and user profile REST calls.
 */

class ReaderBookmarkHTTPCalls(
  private val objectMapper: ObjectMapper,
  private val http: HTTPType) : ReaderBookmarkHTTPCallsType {

  private val logger = LoggerFactory.getLogger(ReaderBookmarkHTTPCalls::class.java)

  override fun bookmarksGet(
    uri: URI,
    credentials: AccountAuthenticationCredentials): List<BookmarkAnnotation> {

    val auth =
      AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials)
    val result =
      this.http.get(Option.some(auth), uri, 0L)

    return result.match<List<BookmarkAnnotation>, IOException>(
      { error -> logAndFail(uri, error) },
      { exception -> throw exception.error },
      { success -> deserializeBookmarksFromStream(success.value) })
  }

  override fun bookmarkAdd(
    uri: URI,
    credentials: AccountAuthenticationCredentials,
    bookmark: BookmarkAnnotation) {

    val data =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToBytes(this.objectMapper, bookmark)
    val auth =
      AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials)
    val result =
      this.http.post(Option.some(auth), uri, data, "application/ld+json")

    return result.match<Unit, IOException>(
      { error -> logAndFail(uri, error) },
      { exception -> throw exception.error },
      { })
  }

  override fun syncingEnable(
    uri: URI,
    credentials: AccountAuthenticationCredentials,
    enabled: Boolean) {

    val data =
      serializeSynchronizeEnableData(enabled)
    val auth =
      AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials)
    val result =
      this.http.put(Option.some(auth), uri, data, "vnd.librarysimplified/user-profile+json")

    return result.match<Unit, IOException>(
      { error -> logAndFail(uri, error) },
      { exception -> throw exception.error },
      { })
  }

  override fun syncingIsEnabled(
    uri: URI,
    credentials: AccountAuthenticationCredentials): Boolean {

    val auth =
      AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials)
    val result =
      this.http.get(Option.some(auth), uri, 0L)

    return result.match<Boolean, IOException>(
      { error -> logAndFail(uri, error) },
      { exception -> throw exception.error },
      { success -> deserializeSyncingEnabledFromStream(success.value) })
  }

  private fun <T> logAndFail(uri: URI, error: HTTPResultError<InputStream>): T {
    HTTPProblemReportLogging.logError(
      this.logger, uri, error.message, error.status, error.problemReport)
    throw IOException("${uri} received ${error.status} ${error.message}")
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
      val text : String? = settings.get("simplified:synchronize_annotations").asText()
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
    val data = this.objectMapper.writeValueAsBytes(node)
    return data
  }
}