package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.Method.Delete
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.Method.Post
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.Method.Put
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.accounts.api.setAuthentication
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationsJSON
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkHTTPCallsType
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A minimal class around the various annotations and user profile REST calls.
 */

class ReaderBookmarkHTTPCalls(
  private val objectMapper: ObjectMapper,
  private val http: LSHTTPClientType
) : ReaderBookmarkHTTPCallsType {

  private val logger = LoggerFactory.getLogger(ReaderBookmarkHTTPCalls::class.java)

  override fun bookmarksGet(
    annotationsURI: URI,
    account: AccountReadableType
  ): List<BookmarkAnnotation> {
    val request =
      this.http.newRequest(annotationsURI)
        .setAuthentication(account)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        this.deserializeBookmarksFromStream(status.bodyStream ?: this.emptyStream())
      is LSHTTPResponseStatus.Responded.Error ->
        this.logAndFail(annotationsURI, status)
      is LSHTTPResponseStatus.Failed ->
        throw status.exception
    }
  }

  override fun bookmarkDelete(
    bookmarkURI: URI,
    account: AccountReadableType
  ) {
    val request =
      this.http.newRequest(bookmarkURI)
        .setAuthentication(account)
        .setMethod(Delete)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK -> {
        this.deserializeBookmarksFromStream(status.bodyStream ?: this.emptyStream())
        Unit
      }
      is LSHTTPResponseStatus.Responded.Error ->
        this.logAndFail(bookmarkURI, status)
      is LSHTTPResponseStatus.Failed ->
        throw status.exception
    }
  }

  private fun emptyStream() = ByteArrayInputStream(ByteArray(0))

  override fun bookmarkAdd(
    annotationsURI: URI,
    bookmark: BookmarkAnnotation,
    account: AccountReadableType
  ) {
    val data =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToBytes(this.objectMapper, bookmark)
    val post =
      Post(data, MIMEType("application", "ld+json", mapOf()))
    val request =
      this.http.newRequest(annotationsURI)
        .setAuthentication(account)
        .setMethod(post)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        Unit
      is LSHTTPResponseStatus.Responded.Error ->
        this.logAndFail(annotationsURI, status)
      is LSHTTPResponseStatus.Failed ->
        throw status.exception
    }
  }

  override fun syncingEnable(
    settingsURI: URI,
    account: AccountReadableType,
    enabled: Boolean
  ) {
    val data =
      this.serializeSynchronizeEnableData(enabled)
    val put =
      Put(data, MIMEType("vnd.librarysimplified", "user-profile+json", mapOf()))
    val request =
      this.http.newRequest(settingsURI)
        .setAuthentication(account)
        .setMethod(put)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        Unit
      is LSHTTPResponseStatus.Responded.Error ->
        this.logAndFail(settingsURI, status)
      is LSHTTPResponseStatus.Failed ->
        throw status.exception
    }
  }

  override fun syncingIsEnabled(
    settingsURI: URI,
    account: AccountReadableType
  ): Boolean {
    val request =
      this.http.newRequest(settingsURI)
        .setAuthentication(account)
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        this.deserializeSyncingEnabledFromStream(status.bodyStream ?: emptyStream())
      is LSHTTPResponseStatus.Responded.Error ->
        this.logAndFail(settingsURI, status)
      is LSHTTPResponseStatus.Failed ->
        throw status.exception
    }
  }

  private fun <T> logAndFail(
    uri: URI,
    error: LSHTTPResponseStatus.Responded.Error
  ): T {
    val problemReport = error.properties.problemReport
    if (problemReport != null) {
      this.logger.error("detail: {}", problemReport.detail)
      this.logger.error("status: {}", problemReport.status)
      this.logger.error("title:  {}", problemReport.title)
      this.logger.error("type:   {}", problemReport.type)
    }
    throw IOException("$uri received ${error.properties.status} ${error.properties.message}")
  }

  private fun deserializeBookmarksFromStream(value: InputStream): List<BookmarkAnnotation> {
    val node =
      this.objectMapper.readTree(value)
    val response =
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationResponseFromJSON(
        objectMapper = this.objectMapper,
        node = node
      )
    return response.first.items
  }

  private fun deserializeSyncingEnabledFromStream(value: InputStream): Boolean {
    return this.deserializeSyncingEnabledFromJSON(this.objectMapper.readTree(value))
  }

  private fun deserializeSyncingEnabledFromJSON(node: JsonNode): Boolean {
    return this.deserializeSyncingEnabledFromJSONObject(JSONParserUtilities.checkObject(null, node))
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
