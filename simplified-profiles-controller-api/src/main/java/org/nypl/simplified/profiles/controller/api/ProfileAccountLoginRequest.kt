package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription

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
    val username: AccountPIN,
    val password: AccountBarcode
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
}
