package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType
import org.nypl.simplified.accounts.json.AccountAuthenticationCredentialsJSON.serializeToJSON
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Functions to serialize and deserialize bundled credentials.
 */

object AccountBundledCredentialsJSON {

  /**
   * Deserialize bundled credentials from the given JSON.
   *
   * @param node The JSON
   * @return A set of bundled credentials
   * @throws JSONParseException On JSON errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    node: ObjectNode
  ): AccountBundledCredentialsType {
    val credentialsByProvider =
      ConcurrentHashMap<URI, AccountAuthenticationCredentials>()
    val byProvider =
      JSONParserUtilities.getObject(node, "credentialsByProvider")
    val iter = byProvider.fieldNames()
    while (iter.hasNext()) {
      val name = iter.next()
      val providerNode =
        JSONParserUtilities.getObject(byProvider, name)
      val credentials =
        AccountAuthenticationCredentialsJSON.deserializeFromJSON(providerNode)
      credentialsByProvider[URI.create(name)] = credentials
    }
    return BundledCredentials(credentialsByProvider)
  }

  /**
   * Deserialize bundled credentials from the given JSON.
   *
   * @param node The JSON
   * @return A set of bundled credentials
   * @throws JSONParseException On JSON errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    node: JsonNode
  ): AccountBundledCredentialsType {
    return deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }

  /**
   * Deserialize bundled credentials from the given JSON.
   *
   * @param mapper The JSON mapper
   * @param stream The input stream
   * @return A set of bundled credentials
   * @throws JSONParseException On JSON errors
   */

  @JvmStatic
  @Throws(IOException::class)
  fun deserializeFromStream(
    mapper: ObjectMapper,
    stream: InputStream
  ): AccountBundledCredentialsType {
    return deserializeFromJSON(mapper.readTree(stream))
  }

  /**
   * Serialize the given credentials to JSON.
   *
   * @param mapper The JSON mapper
   * @param credentials The bundled credentials
   * @return The JSON node
   */

  fun serializeToJSON(
    mapper: ObjectMapper,
    credentials: AccountBundledCredentialsType
  ): ObjectNode {
    val nodeRoot = mapper.createObjectNode()
    val nodeByProvider = mapper.createObjectNode()
    val byProvider =
      credentials.bundledCredentials()
    for (name in byProvider.keys) {
      val creds = byProvider[name]
      val nodeCreds = serializeToJSON(creds!!)
      nodeByProvider.set<JsonNode>(name.toString(), nodeCreds)
    }
    nodeRoot.set<JsonNode>("credentialsByProvider", nodeByProvider)
    return nodeRoot
  }

  /**
   * Serialize the given credentials to JSON.
   *
   * @param mapper The JSON mapper
   * @param credentials The bundled credentials
   * @return The JSON node
   * @throws JsonProcessingException On serialization errors
   */

  @Throws(JsonProcessingException::class)
  fun serializeToBytes(
    mapper: ObjectMapper,
    credentials: AccountBundledCredentialsType
  ): ByteArray {
    return mapper.writeValueAsBytes(serializeToJSON(mapper, credentials))
  }

  /**
   * Serialize the given credentials to JSON.
   *
   * @param mapper The JSON mapper
   * @param credentials The bundled credentials
   * @param stream The output stream
   *
   * @throws IOException On serialization and I/O errors
   */

  @JvmStatic
  @Throws(IOException::class)
  fun serializeToStream(
    mapper: ObjectMapper,
    credentials: AccountBundledCredentialsType,
    stream: OutputStream
  ) {
    mapper.writeValue(stream, serializeToJSON(mapper, credentials))
  }

  data class BundledCredentials internal constructor(
    val credentialsByProvider: Map<URI, AccountAuthenticationCredentials>
  ) : AccountBundledCredentialsType {

    override fun bundledCredentials(): Map<URI, AccountAuthenticationCredentials> {
      return credentialsByProvider
    }

    override fun bundledCredentialsFor(
      accountProvider: URI
    ): AccountAuthenticationCredentials? {
      return this.credentialsByProvider[accountProvider]
    }
  }
}
