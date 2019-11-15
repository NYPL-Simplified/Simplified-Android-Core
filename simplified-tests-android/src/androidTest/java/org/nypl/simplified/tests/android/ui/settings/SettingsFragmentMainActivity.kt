package org.nypl.simplified.tests.android.ui.settings

import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.ui.settings.SettingsFragmentMain

class SettingsFragmentMainActivity : NavigationHostActivity<SettingsFragmentMain>() {
  override fun createFragment(): SettingsFragmentMain =
    SettingsFragmentMain()
}