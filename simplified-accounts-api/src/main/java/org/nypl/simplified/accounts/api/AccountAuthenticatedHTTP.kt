package org.nypl.simplified.accounts.api

import org.librarysimplified.http.api.LSHTTPAuthorizationBasic
import org.librarysimplified.http.api.LSHTTPAuthorizationBearerToken
import org.librarysimplified.http.api.LSHTTPAuthorizationType

/**
 * Convenient functions to construct authenticated HTTP instances from sets of credentials.
 */

object AccountAuthenticatedHTTP {

  fun createAuthorization(
    credentials: AccountAuthenticationCredentials
  ): LSHTTPAuthorizationType {
    return when (credentials) {
      is AccountAuthenticationCredentials.Basic ->
        LSHTTPAuthorizationBasic.ofUsernamePassword(
          userName = credentials.userName.value,
          password = credentials.password.value
        )
      is AccountAuthenticationCredentials.OAuthWithIntermediary ->
        LSHTTPAuthorizationBearerToken.ofToken(
          token = credentials.accessToken
        )
      is AccountAuthenticationCredentials.SAML2_0 ->
        LSHTTPAuthorizationBearerToken.ofToken(
          token = credentials.accessToken
        )
    }
  }

  fun createAuthorizationIfPresent(
    credentials: AccountAuthenticationCredentials?
  ): LSHTTPAuthorizationType? {
    return credentials?.let(this::createAuthorization)
  }
}
