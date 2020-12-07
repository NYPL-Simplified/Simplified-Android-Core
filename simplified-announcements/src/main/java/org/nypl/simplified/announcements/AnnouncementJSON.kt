package org.nypl.simplified.announcements

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import java.util.UUID

/**
 * Functions to serialize/deserialize announcements.
 */

object AnnouncementJSON {

  /**
   * Serialize the given preferences to a JSON object.
   */

  fun serializeToJSON(
    objectMapper: ObjectMapper,
    announcement: Announcement
  ): ObjectNode {
    val node = objectMapper.createObjectNode()
    node.put("id", announcement.id.toString())
    node.put("content", announcement.content)
    announcement.expires?.let {
      node.put(
        "expires",
        ISODateTimeFormat.dateTime()
          .print(it.toDateTime(DateTimeZone.UTC))
      )
    }
    return node
  }

  /**
   * Deserialize an announcement from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: ObjectNode): Announcement {
    val id =
      UUID.fromString(JSONParserUtilities.getString(node, "id"))
    val text =
      JSONParserUtilities.getString(node, "content")
    val expires =
      if (node.has("expires")) {
        JSONParserUtilities.getTimestamp(node, "expires")
          .toLocalDateTime()
      } else {
        null
      }
    return Announcement(
      id = id,
      content = text,
      expires = expires
    )
  }

  /**
   * Deserialize an announcement from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): Announcement {
    return deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }
}
