package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Some
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities

/**
 * Functions for serializing/deserializing account credentials.
 */

object AccountAuthenticationCredentialsJSON {

  /**
   * The version number that will be inferred if no version number is present.
   */

  private const val inferredVersion = 20190424

  /**
   * The current supported version.
   */

  private const val currentSupportedVersion = 20200604

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

    when (credentials) {
      is AccountAuthenticationCredentials.Basic -> {
        authObject.put("@type", "basic")
        authObject.put("username", credentials.userName.value)
        authObject.put("password", credentials.password.value)
      }
      is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
        authObject.put("@type", "oauthWithIntermediary")
        authObject.put("accessToken", credentials.accessToken)
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
    val obj = JSONParserUtilities.checkObject(null, node)
    return when (
      val version =
        JSONParserUtilities.getIntegerDefault(obj, "@version", this.inferredVersion)
    ) {
      this.inferredVersion ->
        this.deserialize20190424(obj)
      20200604 ->
        this.deserialize20200604(obj)
      else ->
        throw JSONParseException("Unsupported version $version")
    }
  }

  @Throws(JSONParseException::class)
  private fun deserialize20200604(
    obj: ObjectNode
  ): AccountAuthenticationCredentials {
    return when (val type = JSONParserUtilities.getString(obj, "@type")) {
      "basic" ->
        this.deserialize20200604Basic(obj)
      "oauthWithIntermediary" ->
        this.deserialize20200604OAuthWithIntermediary(obj)
      else ->
        throw JSONParseException("Unrecognized type: $type")
    }
  }

  private fun deserialize20200604OAuthWithIntermediary(
    obj: ObjectNode
  ): AccountAuthenticationCredentials.OAuthWithIntermediary {
    val adobeCredentials =
      JSONParserUtilities.getObjectOrNull(obj, "adobe_credentials")
        ?.let(this::deserializeAdobeCredentials)

    return AccountAuthenticationCredentials.OAuthWithIntermediary(
      accessToken = JSONParserUtilities.getString(obj, "accessToken"),
      adobeCredentials = adobeCredentials,
      authenticationDescription = JSONParserUtilities.getStringOrNull(obj, "authenticationDescription")
    )
  }

  private fun deserialize20200604Basic(
    obj: ObjectNode
  ): AccountAuthenticationCredentials.Basic {
    val adobeCredentials =
      JSONParserUtilities.getObjectOrNull(obj, "adobe_credentials")
        ?.let(this::deserializeAdobeCredentials)

    return AccountAuthenticationCredentials.Basic(
      userName = AccountUsername(JSONParserUtilities.getString(obj, "username")),
      password = AccountPassword(JSONParserUtilities.getString(obj, "password")),
      adobeCredentials = adobeCredentials,
      authenticationDescription = JSONParserUtilities.getStringOrNull(obj, "authenticationDescription")
    )
  }

  @Throws(JSONParseException::class)
  private fun deserialize20190424(
    obj: ObjectNode
  ): AccountAuthenticationCredentials {
    val user =
      AccountUsername(JSONParserUtilities.getString(obj, "username"))
    val pass =
      AccountPassword(JSONParserUtilities.getString(obj, "password"))
    val adobeCredentials =
      JSONParserUtilities.getObjectOrNull(obj, "adobe_credentials")
        ?.let(this::deserializeAdobeCredentials)

    return AccountAuthenticationCredentials.Basic(
      userName = user,
      password = pass,
      adobeCredentials = adobeCredentials,
      authenticationDescription = null
    )
  }

  private fun deserializeAdobeCredentials(
    credsObj: ObjectNode
  ): AccountAuthenticationAdobePreActivationCredentials {
    val activationOpt =
      JSONParserUtilities.getObjectOptional(credsObj, "activation")

    val credsPost: AccountAuthenticationAdobePostActivationCredentials? =
      if (activationOpt.isSome) {
        val activation =
          (activationOpt as Some<ObjectNode>).get()
        AccountAuthenticationAdobePostActivationCredentials(
          AdobeDeviceID(JSONParserUtilities.getString(activation, "device_id")),
          AdobeUserID(JSONParserUtilities.getString(activation, "user_id"))
        )
      } else {
        null
      }

    return AccountAuthenticationAdobePreActivationCredentials(
      AdobeVendorID(
        JSONParserUtilities.getString(credsObj, "vendor_id")
      ),
      AccountAuthenticationAdobeClientToken.parse(
        JSONParserUtilities.getString(credsObj, "client_token")
      ),
      JSONParserUtilities.getURIOrNull(credsObj, "device_manager_uri"),
      credsPost
    )
  }
}
