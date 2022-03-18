package org.nypl.simplified.accounts.api

import org.librarysimplified.http.api.LSHTTPAuthorizationBasic
import org.librarysimplified.http.api.LSHTTPAuthorizationBearerToken
import org.librarysimplified.http.api.LSHTTPRequestBuilderType
import org.librarysimplified.http.oauth_client_credentials.setOAuthAuthenticateURI

/**
 * Authenticate a request in the best way, which means that OAuth Client Credentials will be used
 * if possible.
 *
 * This must not be used outside of the LoggedIn state when credentials are not available.
 */

fun LSHTTPRequestBuilderType.setAuthentication(account: AccountReadableType): LSHTTPRequestBuilderType {
  val credentials = (account.loginState as? AccountLoginState.AccountLoggedIn)?.credentials
  setAuthorization(credentials)

  val oauthAuthenticationDescription =
    account.provider.authenticationAlternatives
      .filterIsInstance(AccountProviderAuthenticationDescription.OAuthClientCredentials::class.java)
      .firstOrNull()

  oauthAuthenticationDescription?.let {
    setOAuthAuthenticateURI(it.authenticate)
  }

  return this
}

/**
 * Authorize a request on the basis of the given credentials.
 *
 * This is intended to be used outside of the LoggedIn state, when the account doesn't
 * own credentials yet.
 */

fun LSHTTPRequestBuilderType.setAuthorization(credentials: AccountAuthenticationCredentials?): LSHTTPRequestBuilderType {
  val authorization = when (credentials) {
    null -> null
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
  setAuthorization(authorization)
  return this
}
