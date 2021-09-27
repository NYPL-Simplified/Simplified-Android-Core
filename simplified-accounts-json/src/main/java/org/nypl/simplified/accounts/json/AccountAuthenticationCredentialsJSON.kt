package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.json.internal.AccountAuthenticationCredentialsJSON20190424
import org.nypl.simplified.accounts.json.internal.AccountAuthenticationCredentialsJSON20200604
import org.nypl.simplified.accounts.json.internal.AccountAuthenticationCredentialsJSON20200805
import org.nypl.simplified.accounts.json.internal.AccountAuthenticationCredentialsJSON20210512
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities

/**
 * Functions for serializing/deserializing account credentials.
 */

object AccountAuthenticationCredentialsJSON {

  /**
   * The available format versions.
   */

  private val versions = listOf(
    AccountAuthenticationCredentialsJSON20190424,
    AccountAuthenticationCredentialsJSON20200604,
    AccountAuthenticationCredentialsJSON20200805,
    AccountAuthenticationCredentialsJSON20210512
  )

  /**
   * The version number that will be inferred if no version number is present.
   */

  private val inferredVersion =
    this.versions[0].supportedVersion

  /**
   * The current supported version.
   */

  private val currentSupportedVersion =
    this.versions.last().supportedVersion

  /**
   * Serialize the given credentials to a JSON object.
   *
   * @param credentials The credentials.
   * @return A JSON object
   */

  fun serializeToJSON(
    credentials: AccountAuthenticationCredentials
  ): ObjectNode {
    val objectMapper = ObjectMapper()
    val authObject = objectMapper.createObjectNode()
    authObject.put("@version", this.currentSupportedVersion)
    authObject.put("authenticationDescription", credentials.authenticationDescription)

    credentials.annotationsURI?.let { uri ->
      authObject.put("annotationsURI", uri.toString())
    }

    val ignored = when (credentials) {
      is AccountAuthenticationCredentials.Basic -> {
        authObject.put("@type", "basic")
        authObject.put("username", credentials.userName.value)
        authObject.put("password", credentials.password.value)
      }
      is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
        authObject.put("@type", "oauthWithIntermediary")
        authObject.put("accessToken", credentials.accessToken)
      }
      is AccountAuthenticationCredentials.SAML2_0 -> {
        authObject.put("@type", "saml2_0")
        authObject.put("accessToken", credentials.accessToken)
        authObject.put("patronInfo", credentials.patronInfo)

        val cookieArray = objectMapper.createArrayNode()
        for (cookie in credentials.cookies) {
          val cookieObject = objectMapper.createObjectNode()
          cookieObject.put("url", cookie.url)
          cookieObject.put("value", cookie.value)

          cookieArray.add(cookieObject)
        }
        authObject.set("cookies", cookieArray)
      }
    }

    val adobeObj =
      this.serializeAdobeCredentials(
        objectMapper = objectMapper,
        adobe = credentials.adobeCredentials
      )
    if (adobeObj != null) {
      authObject.set<ObjectNode>("adobe_credentials", adobeObj)
    }
    return authObject
  }

  private fun serializeAdobeCredentials(
    objectMapper: ObjectMapper,
    adobe: AccountAuthenticationAdobePreActivationCredentials?
  ): ObjectNode? {
    return if (adobe != null) {
      val adobePreObj = objectMapper.createObjectNode()
      adobePreObj.put("client_token", adobe.clientToken.rawToken)
      adobePreObj.put("vendor_id", adobe.vendorID.value)
      if (adobe.deviceManagerURI != null) {
        adobePreObj.put("device_manager_uri", adobe.deviceManagerURI.toString())
      }
      val post = adobe.postActivationCredentials
      if (post != null) {
        val adobePostObj = objectMapper.createObjectNode()
        adobePostObj.put("device_id", post.deviceID.value)
        adobePostObj.put("user_id", post.userID.value)
        adobePreObj.set<JsonNode>("activation", adobePostObj)
      }
      adobePreObj
    } else {
      null
    }
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
  fun deserializeFromJSON(
    node: JsonNode
  ): AccountAuthenticationCredentials {
    val obj =
      JSONParserUtilities.checkObject(null, node)
    val version =
      JSONParserUtilities.getIntegerDefault(obj, "@version", this.inferredVersion)
    val versionSupport =
      this.versions.find { versioned -> versioned.supportedVersion == version }
        ?: throw JSONParseException("Unsupported version $version")
    return versionSupport.deserializeFromJSON(obj)
  }
}
