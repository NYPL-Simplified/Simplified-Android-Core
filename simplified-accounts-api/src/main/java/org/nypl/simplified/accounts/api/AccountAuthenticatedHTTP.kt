package org.nypl.simplified.accounts.api

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.http.core.HTTPAuthBasic
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPOAuthToken

/**
 * Convenient functions to construct authenticated HTTP instances from sets of credentials.
 */

object AccountAuthenticatedHTTP {

  /**
   * Create an authenticated HTTP instance from the given credentials.
   *
   * @param credentials The credentials
   * @return An HTTP auth instance
   */

  fun createAuthenticatedHTTP(credentials: AccountAuthenticationCredentials): HTTPAuthType {
    return when (credentials) {
      is AccountAuthenticationCredentials.Basic ->
        HTTPAuthBasic.create(
          credentials.userName.value,
          credentials.password.value
        )

      is AccountAuthenticationCredentials.OAuthWithIntermediary ->
        HTTPAuthOAuth.create(HTTPOAuthToken.create(credentials.accessToken))
    }
  }

  /**
   * Create an authenticated HTTP instance from the given credentials, if there are any.
   *
   * @param credentials The credentials
   * @return An HTTP auth instance
   */

  fun createAuthenticatedHTTPOptional(
    credentials: AccountAuthenticationCredentials?
  ): OptionType<HTTPAuthType> {
    return if (credentials == null) {
      Option.none()
    } else {
      Option.some(createAuthenticatedHTTP(credentials))
    }
  }
}
