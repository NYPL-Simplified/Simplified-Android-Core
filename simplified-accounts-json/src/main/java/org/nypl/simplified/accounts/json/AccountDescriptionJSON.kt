package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.nypl.simplified.accounts.api.AccountDescription
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * Functions to serialize and deserialize account descriptions to/from JSON.
 */

object AccountDescriptionJSON {

  private val logger = LoggerFactory.getLogger(AccountDescriptionJSON::class.java)

  /**
   * Deserialize a account description from the given file.
   *
   * @param objectMapper A JSON object mapper
   * @param accountProviders A function use to look up account providers
   * @param file A file
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  @Throws(IOException::class)
  fun deserializeFromFile(
    objectMapper: ObjectMapper,
    accountProviders: (String) -> AccountProviderType?,
    file: File
  ): AccountDescription {
    return deserializeFromText(
      objectMapper = objectMapper,
      accountProviders = accountProviders,
      text = FileUtilities.fileReadUTF8(file)
    )
  }

  /**
   * Deserialize a account description from the given text.
   *
   * @param objectMapper A JSON object mapper
   * @param accountProviders A function use to look up account providers
   * @param text A JSON string
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  @Throws(IOException::class)
  fun deserializeFromText(
    objectMapper: ObjectMapper,
    accountProviders: (String) -> AccountProviderType?,
    text: String
  ): AccountDescription {
    return deserializeFromJSON(
      accountProviders = accountProviders,
      node = objectMapper.readTree(text)
    )
  }

  /**
   * Deserialize a account description from the given JSON node.
   *
   * @param accountProviders A function use to look up account providers
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    accountProviders: (String) -> AccountProviderType?,
    node: JsonNode
  ): AccountDescription {
    val obj = JSONParserUtilities.checkObject(null, node)

    val preferences: AccountPreferences
    if (obj.has("preferences")) {
      preferences = AccountPreferencesJSON.deserializeFromJSON(
        JSONParserUtilities.getObject(obj, "preferences")
      )
    } else {
      preferences = AccountPreferences.defaultPreferences()
    }

    val accountProvider: AccountProviderType =
      when (val value = obj["provider"]) {
        is ObjectNode ->
          AccountProvidersJSON.deserializeFromJSON(
            JSONParserUtilities.getObject(obj, "provider")
          )

        is TextNode -> {
          this.logger.warn("encountered old-style provider ID in account JSON")

          val providerId =
            JSONParserUtilities.getString(obj, "provider")
          accountProviders.invoke(providerId)
            ?: throw JSONParseException("Unresolvable account provider $providerId encountered")
        }

        else -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key 'provider' with a value of type Object|String\n")
          sb.append("Got: A value of type ${value.nodeType}")
          throw JSONParseException(sb.toString())
        }
      }

    return AccountDescription.builder(accountProvider, preferences).build()
  }

  /**
   * Serialize a account description to JSON.
   *
   * @param objectMapper A JSON object mapper
   * @return A serialized object
   */

  fun serializeToJSON(
    objectMapper: ObjectMapper,
    description: AccountDescription
  ): ObjectNode {
    val objectNode = objectMapper.createObjectNode()
    objectNode.put("@version", 20191204)
    objectNode.set<JsonNode>(
      "provider",
      AccountProvidersJSON.serializeToJSON(description.provider())
    )
    objectNode.set<JsonNode>(
      "preferences",
      AccountPreferencesJSON.serializeToJSON(objectMapper, description.preferences())
    )

    return objectNode
  }

  /**
   * Serialize a account description to a JSON string.
   *
   * @param objectMapper A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  @Throws(IOException::class)
  fun serializeToString(
    objectMapper: ObjectMapper,
    description: AccountDescription
  ): String {
    val objectNode = serializeToJSON(objectMapper, description)
    return ByteArrayOutputStream(1024).use { stream ->
      JSONSerializerUtilities.serialize(objectNode, stream)
      stream.toString("UTF-8")
    }
  }
}
