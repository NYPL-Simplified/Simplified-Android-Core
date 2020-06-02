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
   * The authentication description that will be used to log in.
   */

  abstract val description: AccountProviderAuthenticationDescription

  /**
   * A request to log in using basic authentication.
   */

  data class Basic(
    override val accountId: AccountID,
    override val description: AccountProviderAuthenticationDescription.Basic,
    val username: AccountPIN,
    val password: AccountBarcode
  ) : ProfileAccountLoginRequest()

  /**
   * A request to begin a login using OAuth (with an intermediary) authentication.
   */

  data class OAuthWithIntermediaryInitiate(
    override val accountId: AccountID,
    override val description: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) : ProfileAccountLoginRequest()
}
