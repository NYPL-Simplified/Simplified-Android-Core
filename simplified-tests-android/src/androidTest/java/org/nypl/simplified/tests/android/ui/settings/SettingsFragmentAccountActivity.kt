package org.nypl.simplified.tests.android.ui.settings

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.ui.accounts.AccountFragmentParameters

class SettingsFragmentAccountActivity : NavigationHostActivity<org.nypl.simplified.ui.accounts.AccountFragment>() {

  companion object {
    lateinit var initialAccountId: AccountID
  }

  override fun createFragment(): org.nypl.simplified.ui.accounts.AccountFragment =
    org.nypl.simplified.ui.accounts.AccountFragment.create(
      AccountFragmentParameters(
        accountId = initialAccountId
      )
    )
}
