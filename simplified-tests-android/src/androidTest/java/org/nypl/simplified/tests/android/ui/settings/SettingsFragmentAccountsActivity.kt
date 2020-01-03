package org.nypl.simplified.tests.android.ui.settings

import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.ui.settings.SettingsFragmentAccounts

class SettingsFragmentAccountsActivity : NavigationHostActivity<SettingsFragmentAccounts>() {
  override fun createFragment(): SettingsFragmentAccounts =
    SettingsFragmentAccounts()
}