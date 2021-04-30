package org.nypl.simplified.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.accounts.api.AccountID

/**
 * A factory for account view state.
 */

class AccountViewModelFactory(
  private val account: AccountID,
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == AccountDetailViewModel::class.java) {
      return AccountDetailViewModel(
        accountId = this.account
      ) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
