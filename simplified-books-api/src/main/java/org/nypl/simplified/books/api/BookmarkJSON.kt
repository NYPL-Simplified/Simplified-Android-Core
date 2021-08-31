package org.nypl.simplified.books.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.nypl.simplified.opds.core.getOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Functions to serialize bookmarks to/from JSON.
 */

object BookmarkJSON {

  private val dateFormatter =
    ISODateTimeFormat.dateTime()
      .withZoneUTC()

  private val dateParserWithTimezone =
    ISODateTimeFormat.dateTimeParser()
      .withOffsetParsed()

  private val dateParserWithUTC =
    ISODateTimeFormat.dateTimeParser()
      .withZoneUTC()

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
      20210828 ->
        this.deserializeFromJSON20210828(objectMapper, kind, node)
      20210317 ->
        this.deserializeFromJSON20210828(objectMapper, kind, node)
      null ->
        this.deserializeFromJSONOld(objectMapper, kind, node)
      else ->
        throw JSONParseException("Unsupported bookmark version: $version")
    }
  }

  private fun deserializeFromJSON20210828(
    objectMapper: ObjectMapper,
    kind: BookmarkKind,
    node: ObjectNode
  ): Bookmark {
    val location =
      BookLocationJSON.deserializeFromJSON(
        objectMapper,
        JSONParserUtilities.getObject(node, "location")
      )

    val timeParsed =
      this.parseTime(JSONParserUtilities.getString(node, "time"))

    return Bookmark.create(
      opdsId = JSONParserUtilities.getString(node, "opdsId"),
      kind = kind,
      location = location,
      time = timeParsed,
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
      time = this.parseTime(JSONParserUtilities.getString(node, "time")),
      chapterTitle = JSONParserUtilities.getString(node, "chapterTitle"),
      bookProgress = JSONParserUtilities.getDoubleOptional(node, "bookProgress").getOrNull(),
      uri = this.toNullable(JSONParserUtilities.getURIOptional(node, "uri")),
      deviceID = JSONParserUtilities.getStringDefault(node, "deviceID", null)
    )
  }

  /**
   * Correctly parse a date/time value.
   *
   * This slightly odd function first attempts to parse the incoming string as if it was
   * a date/time string with an included time zone. If the time string turned out not to
   * include a time zone, Joda Time will parse it using the system's default timezone. We
   * then detect that this has happened and, if the current system's timezone isn't UTC,
   * we parse the string *again* but this time assuming a UTC timezone.
   */

  private fun parseTime(
    timeText: String
  ): DateTime {
    val defaultZone = DateTimeZone.getDefault()
    val timeParsedWithZone = this.dateParserWithTimezone.parseDateTime(timeText)
    if (timeParsedWithZone.zone == defaultZone && defaultZone != DateTimeZone.UTC) {
      return this.dateParserWithUTC.parseDateTime(timeText)
    }
    return timeParsedWithZone.toDateTime(DateTimeZone.UTC)
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
    node.put("@version", 20210828)
    node.put("opdsId", description.opdsId)
    val location = BookLocationJSON.serializeToJSON(objectMapper, description.location)
    node.set<ObjectNode>("location", location)
    node.put("time", this.dateFormatter.print(description.time))
    node.put("chapterTitle", description.chapterTitle)
    description.bookProgress?.let { node.put("bookProgress", it) }
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
