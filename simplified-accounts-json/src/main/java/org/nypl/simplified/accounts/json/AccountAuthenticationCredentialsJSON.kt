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
import org.nypl.simplified.accounts.api.AccountAuthenticationProvider
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountPatron
import org.nypl.simplified.http.core.HTTPOAuthToken
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

  private const val currentSupportedVersion = 20190424

  /**
   * Serialize the given credentials to a JSON object.
   *
   * @param credentials The credentials.
   * @return A JSON object
   */

  @JvmStatic
  fun serializeToJSON(
    credentials: AccountAuthenticationCredentials
  ): ObjectNode {

    val jom = ObjectMapper()
    val jo = jom.createObjectNode()

    jo.put("@version", this.currentSupportedVersion)
    jo.put("username", credentials.barcode().value())
    jo.put("password", credentials.pin().value())
    credentials.oAuthToken().map_ { x: HTTPOAuthToken ->
      jo.put("oauth_token", x.value())
    }
    credentials.adobeCredentials().map_ { (vendorID, clientToken, deviceURI, post) ->
      val adobePreObj = jom.createObjectNode()
      adobePreObj.put("client_token", clientToken.tokenRaw())
      adobePreObj.put("vendor_id", vendorID.value)
      if (deviceURI != null) {
        adobePreObj.put("device_manager_uri", deviceURI.toString())
      }
      if (post != null) {
        val adobePostObj = jom.createObjectNode()
        adobePostObj.put("device_id", post.deviceID.value)
        adobePostObj.put("user_id", post.userID.value)
        adobePreObj.set<JsonNode>("activation", adobePostObj)
      }
      jo.set<JsonNode>("adobe_credentials", adobePreObj)
    }
    credentials.authenticationProvider()
      .map_ { x: AccountAuthenticationProvider ->
        jo.put("auth_provider", x.value())
      }
    credentials.patron()
      .map_ { x: AccountPatron -> jo.put("patron", x.value()) }
    return jo
  }

  /**
   * Deserialize the given JSON node, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param node Credentials as a JSON node.
   * @return Account credentials
   * @throws JSONParseException On parse errors
   */

  @JvmStatic
  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    node: JsonNode
  ): AccountAuthenticationCredentials {
    val obj = JSONParserUtilities.checkObject(null, node)
    return when (val version =
      JSONParserUtilities.getIntegerDefault(obj, "@version", this.inferredVersion)) {
      this.inferredVersion -> this.deserialize20190424(obj)
      else -> throw JSONParseException("Unsupported version $version")
    }
  }

  @Throws(JSONParseException::class)
  private fun deserialize20190424(
    obj: ObjectNode
  ): AccountAuthenticationCredentials {
    val user =
      AccountBarcode.create(JSONParserUtilities.getString(obj, "username"))
    val pass =
      AccountPIN.create(JSONParserUtilities.getString(obj, "password"))
    val builder =
      AccountAuthenticationCredentials.builder(pass, user)
    builder.setPatron(
      JSONParserUtilities.getStringOptional(obj, "patron")
        .map { x: String? -> AccountPatron.create(x) }
    )
    builder.setAuthenticationProvider(
      JSONParserUtilities.getStringOptional(obj, "auth_provider")
        .map { x: String? -> AccountAuthenticationProvider.create(x) }
    )
    builder.setOAuthToken(
      JSONParserUtilities.getStringOptional(obj, "oauth_token")
        .map { x: String? -> HTTPOAuthToken.create(x) }
    )
    builder.setAdobeCredentials(
      JSONParserUtilities.getObjectOptional(obj, "adobe_credentials")
        .mapPartial<AccountAuthenticationAdobePreActivationCredentials, JSONParseException> { credsObj: ObjectNode? ->
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

          AccountAuthenticationAdobePreActivationCredentials(
            AdobeVendorID(JSONParserUtilities.getString(credsObj, "vendor_id")),
            AccountAuthenticationAdobeClientToken.create(
              JSONParserUtilities.getString(credsObj, "client_token")
            ),
            JSONParserUtilities.getURIOrNull(credsObj, "device_manager_uri"),
            credsPost
          )
        }
    )
    return builder.build()
  }
}
