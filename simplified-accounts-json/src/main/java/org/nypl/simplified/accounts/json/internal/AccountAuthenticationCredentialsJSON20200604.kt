package org.nypl.simplified.accounts.json.internal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.slf4j.LoggerFactory

/**
 * JSON format version 20200604.
 */

object AccountAuthenticationCredentialsJSON20200604 : AccountAuthenticationCredentialsJSONVersionedType {

  private val logger =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsJSON20200604::class.java)

  override val supportedVersion: Int =
    20200604

  override fun deserializeFromJSON(
    node: JsonNode
  ): AccountAuthenticationCredentials {
    logger.debug("deserializing version 20200604")

    val obj =
      JSONParserUtilities.checkObject(null, node)

    return when (val type = JSONParserUtilities.getString(obj, "@type")) {
      "basic" ->
        deserializeBasic(obj)
      "oauthWithIntermediary" ->
        deserializeOAuthWithIntermediary(obj)
      else ->
        throw JSONParseException("Unrecognized type: $type")
    }
  }

  private fun deserializeOAuthWithIntermediary(
    obj: ObjectNode
  ): AccountAuthenticationCredentials.OAuthWithIntermediary {
    val adobeCredentials =
      JSONParserUtilities.getObjectOrNull(obj, "adobe_credentials")
        ?.let(AccountAuthenticationCredentialsAdobeJSON::deserializeAdobeCredentials)

    return AccountAuthenticationCredentials.OAuthWithIntermediary(
      accessToken = JSONParserUtilities.getString(obj, "accessToken"),
      adobeCredentials = adobeCredentials,
      authenticationDescription = JSONParserUtilities.getStringOrNull(obj, "authenticationDescription"),
      annotationsURI = null
    )
  }

  private fun deserializeBasic(
    obj: ObjectNode
  ): AccountAuthenticationCredentials.Basic {
    val adobeCredentials =
      JSONParserUtilities.getObjectOrNull(obj, "adobe_credentials")
        ?.let(AccountAuthenticationCredentialsAdobeJSON::deserializeAdobeCredentials)

    return AccountAuthenticationCredentials.Basic(
      userName = AccountUsername(JSONParserUtilities.getString(obj, "username")),
      password = AccountPassword(JSONParserUtilities.getString(obj, "password")),
      adobeCredentials = adobeCredentials,
      authenticationDescription = JSONParserUtilities.getStringOrNull(obj, "authenticationDescription"),
      annotationsURI = null
    )
  }
}
