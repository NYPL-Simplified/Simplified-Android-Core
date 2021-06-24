package org.nypl.simplified.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.listeners.api.FragmentListenerType

/**
 * A factory for account view state.
 */

class AccountDetailViewModelFactory(
  private val account: AccountID,
  private val listener: FragmentListenerType<AccountDetailEvent>
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == AccountDetailViewModel::class.java) {
      return AccountDetailViewModel(
        accountId = this.account,
        listener = this.listener
      ) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
