package org.nypl.simplified.ui.accounts

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

sealed class AccountListEvent {

  /**
   * An existing account has been selected.
   */

  data class AccountSelected(
    val account: AccountID
  ) : AccountListEvent()

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : AccountListEvent()

  /**
   * The patron wants to add a new account.
   */

  object AddAccount : AccountListEvent()
}
