package org.nypl.simplified.books.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.joda.time.LocalDateTime
import org.joda.time.Seconds
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Functions to serialize/deserialize bearer tokens.
 */

object SimplifiedBearerTokenJSON {

  fun serializeToJSON(
    objectMapper: ObjectMapper,
    now: LocalDateTime,
    token: SimplifiedBearerToken
  ): ObjectNode {
    val expires = Seconds.secondsBetween(now, token.expiration).seconds
    val node = objectMapper.createObjectNode()
    node.put("access_token", token.accessToken)
    node.put("expires_in", expires)
    node.put("location", token.location.toString())
    return node
  }

  fun serializeToText(
    objectMapper: ObjectMapper,
    now: LocalDateTime,
    token: SimplifiedBearerToken
  ): String {
    return ByteArrayOutputStream().use { stream ->
      val writer = objectMapper.writerWithDefaultPrettyPrinter()
      writer.writeValue(stream, serializeToJSON(objectMapper, now, token))
      stream.toString("UTF-8")
    }
  }

  fun serializeToFile(
    objectMapper: ObjectMapper,
    now: LocalDateTime,
    token: SimplifiedBearerToken,
    file: File
  ) {
    FileOutputStream(file).use { stream ->
      val writer = objectMapper.writerWithDefaultPrettyPrinter()
      writer.writeValue(stream, serializeToJSON(objectMapper, now, token))
    }
  }

  fun deserializeFromJSON(
    now: LocalDateTime,
    node: ObjectNode
  ): SimplifiedBearerToken {
    return SimplifiedBearerToken(
      accessToken = JSONParserUtilities.getString(node, "access_token"),
      expiration = now.plusSeconds(JSONParserUtilities.getInteger(node, "expires_in")),
      location = JSONParserUtilities.getURI(node, "location")
    )
  }

  fun deserializeFromJSON(
    now: LocalDateTime,
    node: JsonNode
  ): SimplifiedBearerToken {
    return deserializeFromJSON(now, JSONParserUtilities.checkObject(null, node))
  }

  fun deserializeFromText(
    objectMapper: ObjectMapper,
    now: LocalDateTime,
    text: String
  ): SimplifiedBearerToken {
    return deserializeFromJSON(now, objectMapper.readTree(text))
  }

  fun deserializeFromFile(
    objectMapper: ObjectMapper,
    now: LocalDateTime,
    file: File
  ): SimplifiedBearerToken {
    return deserializeFromText(objectMapper, now, FileUtilities.fileReadUTF8(file))
  }
}
