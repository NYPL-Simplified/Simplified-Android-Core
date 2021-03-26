package org.nypl.simplified.books.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Functions to serialize bookmarks to/from JSON.
 */

object BookmarkJSON {

  private val dateFormatter =
    ISODateTimeFormat.dateTime()

  /**
   * Deserialize bookmarks from the given JSON node.
   *
   * @param objectMapper A JSON object mapper
   * @param kind The kind of bookmark
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  @JvmStatic
  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    objectMapper: ObjectMapper,
    kind: BookmarkKind,
    node: JsonNode
  ): Bookmark {
    return this.deserializeFromJSON(
      objectMapper = objectMapper,
      kind = kind,
      node = JSONParserUtilities.checkObject(null, node)
    )
  }

  /**
   * Deserialize bookmarks from the given JSON node.
   *
   * @param objectMapper A JSON object mapper
   * @param kind The kind of bookmark
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  @JvmStatic
  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    objectMapper: ObjectMapper,
    kind: BookmarkKind,
    node: ObjectNode
  ): Bookmark {
    return when (val version = JSONParserUtilities.getIntegerOrNull(node, "@version")) {
      20210317 ->
        this.deserializeFromJSON20210317(objectMapper, kind, node)
      null ->
        this.deserializeFromJSONOld(objectMapper, kind, node)
      else ->
        throw JSONParseException("Unsupported bookmark version: $version")
    }
  }

  private fun deserializeFromJSON20210317(
    objectMapper: ObjectMapper,
    kind: BookmarkKind,
    node: ObjectNode
  ): Bookmark {
    val location =
      BookLocationJSON.deserializeFromJSON(
        objectMapper,
        JSONParserUtilities.getObject(node, "location")
      )

    return Bookmark.create(
      opdsId = JSONParserUtilities.getString(node, "opdsId"),
      kind = kind,
      location = location,
      time = DateTime.parse(JSONParserUtilities.getString(node, "time")),
      chapterTitle = JSONParserUtilities.getString(node, "chapterTitle"),
      bookProgress = JSONParserUtilities.getDouble(node, "bookProgress"),
      uri = this.toNullable(JSONParserUtilities.getURIOptional(node, "uri")),
      deviceID = JSONParserUtilities.getStringDefault(node, "deviceID", null)
    )
  }

  private fun deserializeFromJSONOld(
    objectMapper: ObjectMapper,
    kind: BookmarkKind,
    node: ObjectNode
  ): Bookmark {
    val location =
      BookLocationJSON.deserializeFromJSON(
        objectMapper,
        JSONParserUtilities.getObject(node, "location")
      )

    /*
     * Old bookmarks have a top-level chapterProgress value. We've moved to having this
     * stored explicitly in book locations for modern bookmarks. We pick whichever is
     * the greater of the two possible values, because we default to 0.0 for missing
     * values.
     */

    val chapterProgress =
      JSONParserUtilities.getDoubleDefault(node, "chapterProgress", 0.0)

    val locationMax =
      when (location) {
        is BookLocation.BookLocationR2 ->
          location
        is BookLocation.BookLocationR1 ->
          location.copy(progress = Math.max(location.progress ?: 0.0, chapterProgress))
      }

    return Bookmark.create(
      opdsId = JSONParserUtilities.getString(node, "opdsId"),
      kind = kind,
      location = locationMax,
      time = DateTime.parse(JSONParserUtilities.getString(node, "time")),
      chapterTitle = JSONParserUtilities.getString(node, "chapterTitle"),
      bookProgress = JSONParserUtilities.getDouble(node, "bookProgress"),
      uri = this.toNullable(JSONParserUtilities.getURIOptional(node, "uri")),
      deviceID = JSONParserUtilities.getStringDefault(node, "deviceID", null)
    )
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
    description: Bookmark
  ): ObjectNode {
    val node = objectMapper.createObjectNode()
    node.put("@version", 20210317)
    node.put("opdsId", description.opdsId)
    val location = BookLocationJSON.serializeToJSON(objectMapper, description.location)
    node.set<ObjectNode>("location", location)
    node.put("time", this.dateFormatter.print(description.time))
    node.put("chapterTitle", description.chapterTitle)
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
    bookmarks: List<Bookmark>
  ): ArrayNode {
    val node = objectMapper.createArrayNode()
    bookmarks.forEach { bookmark -> node.add(this.serializeToJSON(objectMapper, bookmark)) }
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
    description: Bookmark
  ): String {
    val json = this.serializeToJSON(objectMapper, description)
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
    bookmarks: List<Bookmark>
  ): String {
    val json = this.serializeToJSON(objectMapper, bookmarks)
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
    kind: BookmarkKind,
    serialized: String
  ): Bookmark {
    return this.deserializeFromJSON(
      objectMapper = objectMapper,
      kind = kind,
      node = objectMapper.readTree(serialized)
    )
  }
}
