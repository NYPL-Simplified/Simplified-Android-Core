package org.nypl.simplified.books.accounts

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities

/**
 * Functions to serialize/deserialize account preferences.
 */

object AccountPreferencesJSON {

  /**
   * Serialize the given preferences to a JSON object.
   */

  fun serializeToJSON(
    objectMapper: ObjectMapper,
    preferences: AccountPreferences): ObjectNode {

    val node = objectMapper.createObjectNode()
    node.put("bookmarkSyncingPermitted", preferences.bookmarkSyncingPermitted)
    return node
  }

  /**
   * Deserialize preferences from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: ObjectNode): AccountPreferences {
    return AccountPreferences(
      bookmarkSyncingPermitted = JSONParserUtilities.getBoolean(node, "bookmarkSyncingPermitted"))
  }

  /**
   * Deserialize preferences from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): AccountPreferences {
    return deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }

}
