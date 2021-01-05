package org.nypl.simplified.ui.accounts.saml20

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import java.io.File

/**
 * A factory for SAML 2.0 view state.
 */

class AccountSAML20ViewModelFactory(
  private val profiles: ProfilesControllerType,
  private val account: AccountID,
  private val description: AccountProviderAuthenticationDescription.SAML2_0,
  private val resources: Resources,
  private val webViewDataDir: File
) : ViewModelProvider.Factory {

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == AccountSAML20ViewModel::class.java) {
      return AccountSAML20ViewModel(
        account = this.account,
        description = this.description,
        profiles = this.profiles,
        resources = this.resources,
        webViewDataDir = this.webViewDataDir
      ) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
