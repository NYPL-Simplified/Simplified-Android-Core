package org.nypl.simplified.accounts.json.internal

import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Some
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.json.core.JSONParserUtilities

object AccountAuthenticationCredentialsAdobeJSON {

  fun deserializeAdobeCredentials(
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
