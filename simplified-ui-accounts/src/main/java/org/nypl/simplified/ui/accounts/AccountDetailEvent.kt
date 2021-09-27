package org.nypl.simplified.ui.accounts

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

sealed class AccountDetailEvent {

  /**
   * The patron has successfully logged into the account.
   */

  object LoginSucceeded : AccountDetailEvent()

  /**
   * The patron wants to log in through SAML.
   */

  data class OpenSAML20Login(
    val account: AccountID,
    val authenticationDescription: AccountProviderAuthenticationDescription.SAML2_0
  ) : AccountDetailEvent()

  /**
   * Login has failed and the patron wants to see some details about the error.
   */

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : AccountDetailEvent()
}
