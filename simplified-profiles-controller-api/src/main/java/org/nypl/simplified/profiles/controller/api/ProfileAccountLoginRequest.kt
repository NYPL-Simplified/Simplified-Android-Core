package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountCookie
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountUsername

/**
 * A request to log in to an account.
 */

sealed class ProfileAccountLoginRequest {

  /**
   * The ID of the account.
   */

  abstract val accountId: AccountID

  /**
   * A request to log in using basic authentication.
   */

  data class Basic(
    override val accountId: AccountID,
    val description: AccountProviderAuthenticationDescription.Basic,
    val username: AccountUsername,
    val password: AccountPassword
  ) : ProfileAccountLoginRequest()

  /**
   * A request to begin a login using OAuth (with an intermediary) authentication.
   */

  data class OAuthWithIntermediaryInitiate(
    override val accountId: AccountID,
    val description: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) : ProfileAccountLoginRequest()

  /**
   * A request to complete a login using OAuth (with an intermediary) authentication. In other
   * words, an OAuth token has been passed to the application.
   */

  data class OAuthWithIntermediaryComplete(
    override val accountId: AccountID,
    val token: String
  ) : ProfileAccountLoginRequest()

  /**
   * A request to cancel waiting for a login using OAuth (with an intermediary) authentication.
   */

  data class OAuthWithIntermediaryCancel(
    override val accountId: AccountID,
    val description: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) : ProfileAccountLoginRequest()

  /**
   * A request to begin a login using SAML 2.0 authentication.
   */

  data class SAML20Initiate(
    override val accountId: AccountID,
    val description: AccountProviderAuthenticationDescription.SAML2_0
  ) : ProfileAccountLoginRequest()

  /**
   * A request to complete a login using SAML 2.0 authentication. In other
   * words, a set of SAML information has been passed to the application.
   */

  data class SAML20Complete(
    override val accountId: AccountID,
    val accessToken: String,
    val patronInfo: String,
    val cookies: List<AccountCookie>
  ) : ProfileAccountLoginRequest()

  /**
   * A request to cancel waiting for a login using SAML 2.0 authentication.
   */

  data class SAML20Cancel(
    override val accountId: AccountID,
    val description: AccountProviderAuthenticationDescription.SAML2_0
  ) : ProfileAccountLoginRequest()
}
