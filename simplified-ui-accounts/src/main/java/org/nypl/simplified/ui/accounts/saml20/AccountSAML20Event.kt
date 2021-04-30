package org.nypl.simplified.ui.accounts.saml20

import org.nypl.simplified.ui.errorpage.ErrorPageParameters

sealed class AccountSAML20Event {

  /**
   * The patron has successfully logged into the account.
   */

  object AccessTokenObtained : AccountSAML20Event()

  /**
   * Login has failed and the patron wants to see some details about the error.
   */

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : AccountSAML20Event()
}
