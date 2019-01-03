package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.nypl.simplified.http.core.HTTPAuthBasic
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPOAuthToken

/**
 * Functions to configure HTTP auth from a set of account credentials.
 */

class AccountCredentialsHTTP {

  companion object {

    /**
     * @return An HTTP auth value for the given credentials, if any
     */

    fun toHttpAuthOptional(credentials: OptionType<AccountCredentials>): OptionType<HTTPAuthType> {
      return credentials.map { c -> toHttpAuth(c) }
    }

    /**
     * @return An HTTP auth value for the given credentials
     */

    fun toHttpAuth(credentials: AccountCredentials): HTTPAuthType {
      val barcode = credentials.barcode
      val pin = credentials.pin

      val authToken = credentials.authToken
      return if (authToken is Some<AccountAuthToken>) {
        val token = authToken.get()
        if (token != null) {
          HTTPAuthOAuth.create(HTTPOAuthToken.create(token.toString()))
        } else {
          HTTPAuthBasic.create(barcode.toString(), pin.toString())
        }
      } else {
        HTTPAuthBasic.create(barcode.toString(), pin.toString())
      }
    }
  }

}
