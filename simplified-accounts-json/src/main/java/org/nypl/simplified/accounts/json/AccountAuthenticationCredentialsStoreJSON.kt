package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Functions for serializing/deserializing account credentials.
 */

object AccountAuthenticationCredentialsStoreJSON {

  private val logger =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsStoreJSON::class.java)

  /**
   * The version number that will be inferred if no version number is present.
   */

  private const val inferredVersion = 20190424

  /**
   * The current supported version.
   */

  private const val currentSupportedVersion = 20190424

  /**
   * Serialize the given credentials to a JSON object, and serialize that to a
   * UTF-8 string.
   *
   * @param credentials The credentials.
   * @return A string of JSON
   * @throws IOException On I/O or serialization errors
   */

  @Throws(IOException::class)
  fun serializeToText(
    credentials: Map<AccountID, AccountAuthenticationCredentials>
  ): String {
    val jo = serializeToJSON(credentials)
    val bao = ByteArrayOutputStream(1024)
    JSONSerializerUtilities.serialize(jo, bao)
    return bao.toString("UTF-8")
  }

  /**
   * Serialize the given credentials to a JSON object.
   *
   * @param credentials The credentials.
   * @return A JSON object
   */

  fun serializeToJSON(credentials: Map<AccountID, AccountAuthenticationCredentials>): ObjectNode {
    val jom = ObjectMapper()

    val obj = jom.createObjectNode()
    obj.put("@version", currentSupportedVersion)

    val credsObject = jom.createObjectNode()
    for (key in credentials.keys) {
      val item = credentials[key]!!
      val credsObj = AccountAuthenticationCredentialsJSON.serializeToJSON(item)
      credsObject.set<ObjectNode>(key.uuid.toString(), credsObj)
    }

    obj.set<ObjectNode>("credentials", credsObject)
    return obj
  }

  /**
   * Deserialize the given text, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param text The credentials text.
   * @return Account credentials
   * @throws IOException On I/O or serialization errors
   */

  @Throws(IOException::class)
  fun deserializeFromText(text: String): Map<AccountID, AccountAuthenticationCredentials> {
    return deserializeFromJSON(ObjectMapper().readTree(text))
  }

  /**
   * Deserialize the given JSON node, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param node Credentials as a JSON node.
   * @return Account credentials
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): Map<AccountID, AccountAuthenticationCredentials> {
    val obj = JSONParserUtilities.checkObject(null, node)

    return when (
      val version =
        JSONParserUtilities.getIntegerDefault(obj, "@version", inferredVersion)
    ) {
      20190424 ->
        deserializeFromJSONV20190424(obj)
      else ->
        throw JSONParseException("Unsupported credentials version: $version")
    }
  }

  private fun deserializeFromJSONV20190424(
    obj: ObjectNode
  ): Map<AccountID, AccountAuthenticationCredentials> {
    val credentials =
      JSONParserUtilities.getObject(obj, "credentials")
    val result =
      mutableMapOf<AccountID, AccountAuthenticationCredentials>()

    for (key in credentials.fieldNames()) {
      try {
        val accountID =
          AccountID(UUID.fromString(key))
        result[accountID] =
          AccountAuthenticationCredentialsJSON.deserializeFromJSON(credentials.get(key))
      } catch (e: Exception) {
        this.logger.error("error deserializing credential: ", e)
      }
    }

    return result.toMap()
  }
}
