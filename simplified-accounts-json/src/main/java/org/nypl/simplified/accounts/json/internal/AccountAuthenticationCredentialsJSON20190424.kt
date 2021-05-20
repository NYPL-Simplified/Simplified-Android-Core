package org.nypl.simplified.accounts.json.internal

import com.fasterxml.jackson.databind.JsonNode
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.json.core.JSONParserUtilities
import org.slf4j.LoggerFactory

/**
 * JSON format version 20190424.
 */

object AccountAuthenticationCredentialsJSON20190424 : AccountAuthenticationCredentialsJSONVersionedType {

  private val logger =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsJSON20190424::class.java)

  override val supportedVersion: Int =
    20190424

  override fun deserializeFromJSON(
    node: JsonNode
  ): AccountAuthenticationCredentials {
    logger.debug("deserializing version 20190424")

    val obj =
      JSONParserUtilities.checkObject(null, node)
    val user =
      AccountUsername(JSONParserUtilities.getString(obj, "username"))
    val pass =
      AccountPassword(JSONParserUtilities.getString(obj, "password"))
    val adobeCredentials =
      JSONParserUtilities.getObjectOrNull(obj, "adobe_credentials")
        ?.let(AccountAuthenticationCredentialsAdobeJSON::deserializeAdobeCredentials)

    return AccountAuthenticationCredentials.Basic(
      userName = user,
      password = pass,
      adobeCredentials = adobeCredentials,
      authenticationDescription = null,
      annotationsURI = null
    )
  }
}
