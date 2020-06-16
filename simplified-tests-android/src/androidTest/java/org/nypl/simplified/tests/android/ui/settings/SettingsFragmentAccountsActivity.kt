package org.nypl.simplified.tests.android.ui.settings

import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.ui.accounts.AccountsFragment

class SettingsFragmentAccountsActivity : NavigationHostActivity<AccountsFragment>() {
  override fun createFragment(): AccountsFragment =
    AccountsFragment()
}
