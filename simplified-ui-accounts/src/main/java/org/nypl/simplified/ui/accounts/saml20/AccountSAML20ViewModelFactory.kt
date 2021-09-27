package org.nypl.simplified.ui.accounts.saml20

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription

/**
 * A factory for SAML 2.0 view state.
 */

class AccountSAML20ViewModelFactory(
  private val application: Application,
  private val account: AccountID,
  private val description: AccountProviderAuthenticationDescription.SAML2_0,
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == AccountSAML20ViewModel::class.java) {
      return AccountSAML20ViewModel(
        application = this.application,
        account = this.account,
        description = this.description
      ) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
