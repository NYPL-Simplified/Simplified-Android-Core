package org.nypl.simplified.books.reader

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.joda.time.LocalDateTime
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Functions to serialize bookmarks to/from JSON.
 */

object ReaderBookmarkJSON {

  /**
   * Deserialize bookmarks from the given JSON node.
   *
   * @param objectMapper  A JSON object mapper
   * @param kind The kind of bookmark
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  @JvmStatic
  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    objectMapper: ObjectMapper,
    kind: ReaderBookmarkKind,
    node: JsonNode): ReaderBookmark {
    return deserializeFromJSON(
      objectMapper = objectMapper,
      kind = kind,
      node = JSONParserUtilities.checkObject(null, node))
  }

  /**
   * Deserialize bookmarks from the given JSON node.
   *
   * @param objectMapper  A JSON object mapper
   * @param kind The kind of bookmark
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  @JvmStatic
  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    objectMapper: ObjectMapper,
    kind: ReaderBookmarkKind,
    node: ObjectNode): ReaderBookmark {

    return ReaderBookmark(
      opdsId = JSONParserUtilities.getString(node, "opdsId"),
      kind = kind,
      location = ReaderBookLocationJSON.deserializeFromJSON(objectMapper, JSONParserUtilities.getObject(node, "location")),
      time = LocalDateTime.parse(JSONParserUtilities.getString(node, "time")),
      chapterTitle = JSONParserUtilities.getString(node, "chapterTitle"),
      chapterProgress = JSONParserUtilities.getDouble(node, "chapterProgress"),
      bookProgress = JSONParserUtilities.getDouble(node, "bookProgress"),
      uri = toNullable(JSONParserUtilities.getURIOptional(node, "uri")),
      deviceID = JSONParserUtilities.getStringDefault(node, "deviceID", null))
  }

  private fun <T> toNullable(option: OptionType<T>): T? {
    return if (option is Some<T>) {
      option.get()
    } else {
      null
    }
  }

  /**
   * Serialize a bookmark to JSON.
   *
   * @param objectMapper A JSON object mapper
   * @return A serialized object
   */

  @JvmStatic
  fun serializeToJSON(
    objectMapper: ObjectMapper,
    description: ReaderBookmark): ObjectNode {

    val node = objectMapper.createObjectNode()
    node.put("opdsId", description.opdsId)
    node.set("location", ReaderBookLocationJSON.serializeToJSON(objectMapper, description.location))
    node.put("time", description.time.toString())
    node.put("chapterTitle", description.chapterTitle)
    node.put("chapterProgress", description.chapterProgress)
    node.put("bookProgress", description.bookProgress)
    description.deviceID.let { device -> node.put("deviceID", device) }
    return node
  }

  /**
   * Serialize a bookmark to JSON.
   *
   * @param objectMapper A JSON object mapper
   * @return A serialized object
   */

  @JvmStatic
  fun serializeToJSON(
    objectMapper: ObjectMapper,
    bookmarks: List<ReaderBookmark>): ArrayNode {

    val node = objectMapper.createArrayNode()
    bookmarks.forEach { bookmark -> node.add(serializeToJSON(objectMapper, bookmark)) }
    return node
  }

  /**
   * Serialize a bookmark to a JSON string.
   *
   * @param objectMapper A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  @JvmStatic
  @Throws(IOException::class)
  fun serializeToString(
    objectMapper: ObjectMapper,
    description: ReaderBookmark): String {

    val json = serializeToJSON(objectMapper, description)
    val output = ByteArrayOutputStream(1024)
    JSONSerializerUtilities.serialize(json, output)
    return output.toString("UTF-8")
  }

  /**
   * Serialize a bookmark to a JSON string.
   *
   * @param objectMapper A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  @JvmStatic
  @Throws(IOException::class)
  fun serializeToString(
    objectMapper: ObjectMapper,
    bookmarks: List<ReaderBookmark>): String {

    val json = serializeToJSON(objectMapper, bookmarks)
    val output = ByteArrayOutputStream(1024)
    val writer = objectMapper.writerWithDefaultPrettyPrinter()
    writer.writeValue(output, json)
    return output.toString("UTF-8")
  }

  /**
   * Deserialize a bookmark from the given string.
   *
   * @param objectMapper A JSON object mapper
   * @param kind The kind of bookmark
   * @param serialized A serialized JSON string
   * @return A parsed location
   * @throws IOException On I/O or parser errors
   */

  @JvmStatic
  @Throws(IOException::class)
  fun deserializeFromString(
    objectMapper: ObjectMapper,
    kind: ReaderBookmarkKind,
    serialized: String): ReaderBookmark {
    return deserializeFromJSON(
      objectMapper = objectMapper,
      kind = kind,
      node = objectMapper.readTree(serialized)
    )
  }
}
