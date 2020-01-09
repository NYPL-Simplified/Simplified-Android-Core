package org.nypl.simplified.tests.android.ui.settings

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.ui.settings.SettingsFragmentAccount
import org.nypl.simplified.ui.settings.SettingsFragmentAccountParameters

class SettingsFragmentAccountActivity : NavigationHostActivity<SettingsFragmentAccount>() {

  companion object {
    lateinit var initialAccountId: AccountID
  }

  override fun createFragment(): SettingsFragmentAccount =
    SettingsFragmentAccount.create(SettingsFragmentAccountParameters(
      accountId = initialAccountId
    ))
}
