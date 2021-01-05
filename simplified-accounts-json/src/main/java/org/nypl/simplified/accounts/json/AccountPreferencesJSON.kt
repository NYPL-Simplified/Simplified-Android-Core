package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.UUID

/**
 * Functions to serialize/deserialize account preferences.
 */

object AccountPreferencesJSON {

  private val logger =
    LoggerFactory.getLogger(AccountPreferencesJSON::class.java)

  /**
   * Serialize the given preferences to a JSON object.
   */

  fun serializeToJSON(
    objectMapper: ObjectMapper,
    preferences: AccountPreferences
  ): ObjectNode {
    val node = objectMapper.createObjectNode()
    node.put("bookmarkSyncingPermitted", preferences.bookmarkSyncingPermitted)
    preferences.catalogURIOverride?.let {
      node.put("catalogURIOverride", it.toString())
    }

    val acks = objectMapper.createArrayNode()
    for (ack in preferences.announcementsAcknowledged) {
      acks.add(ack.toString())
    }

    node.set<ArrayNode>("announcementsAcknowledged", acks)
    return node
  }

  private fun parseAnnouncementsAcknowledged(node: ObjectNode): List<UUID> {
    try {
      val array = JSONParserUtilities.getArray(node, "announcementsAcknowledged")
      val results = mutableListOf<UUID>()
      for (item in array) {
        try {
          results.add(UUID.fromString(JSONParserUtilities.checkString(item)))
        } catch (e: Exception) {
          this.logger.error("unable to parse acknowledgement: ", e)
        }
      }
      return results.toList()
    } catch (e: Exception) {
      this.logger.error("unable to parse acknowledgements: ", e)
      return emptyList()
    }
  }

  /**
   * Deserialize preferences from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: ObjectNode): AccountPreferences {
    return AccountPreferences(
      bookmarkSyncingPermitted = JSONParserUtilities.getBoolean(node, "bookmarkSyncingPermitted"),
      catalogURIOverride = JSONParserUtilities.getURIOrNull(node, "catalogURIOverride"),
      announcementsAcknowledged = this.parseAnnouncementsAcknowledged(node)
    )
  }

  /**
   * Deserialize preferences from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): AccountPreferences {
    return this.deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }
}
