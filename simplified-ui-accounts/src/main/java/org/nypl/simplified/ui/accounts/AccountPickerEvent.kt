package org.nypl.simplified.ui.accounts

import org.nypl.simplified.accounts.api.AccountID

sealed class AccountPickerEvent {

  /**
   * An existing account has been selected.
   */

  data class AccountSelected(
    val account: AccountID
  ) : AccountPickerEvent()

  /**
   * The patron wants to add a new account.
   */

  object AddAccount : AccountPickerEvent()
}
