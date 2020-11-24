package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountPreferences
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
    preferences: AccountPreferences
  ): ObjectNode {
    val node = objectMapper.createObjectNode()
    node.put("bookmarkSyncingPermitted", preferences.bookmarkSyncingPermitted)
    preferences.catalogURIOverride?.let {
      node.put("catalogURIOverride", it.toString())
    }
    return node
  }

  /**
   * Deserialize preferences from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: ObjectNode): AccountPreferences {
    return AccountPreferences(
      bookmarkSyncingPermitted = JSONParserUtilities.getBoolean(node, "bookmarkSyncingPermitted"),
      catalogURIOverride = JSONParserUtilities.getURIOrNull(node, "catalogURIOverride")
    )
  }

  /**
   * Deserialize preferences from the given JSON object.
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): AccountPreferences {
    return deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }
}
