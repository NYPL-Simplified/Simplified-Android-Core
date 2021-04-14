package org.nypl.simplified.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.navigation.api.NavigationControllers
import org.slf4j.LoggerFactory

/**
 * The main settings page containing links to other settings pages.
 */

class SettingsFragmentMain : PreferenceFragmentCompat() {

  private val logger = LoggerFactory.getLogger(SettingsFragmentMain::class.java)
  private val viewModel: SettingsMainViewModel by viewModels()

  private lateinit var settingsAbout: Preference
  private lateinit var settingsAcknowledgements: Preference
  private lateinit var settingsAccounts: Preference
  private lateinit var settingsBuild: Preference
  private lateinit var settingsDebug: Preference
  private lateinit var settingsEULA: Preference
  private lateinit var settingsFaq: Preference
  private lateinit var settingsLicense: Preference
  private lateinit var settingsVersion: Preference
  private lateinit var settingsVersionCore: Preference

  private var toast: Toast? = null
  private var tapToDebugSettings = 7

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    this.setPreferencesFromResource(R.xml.settings, rootKey)

    this.settingsAbout = this.findPreference("settingsAbout")!!
    this.settingsAcknowledgements = this.findPreference("settingsAcknowledgements")!!
    this.settingsAccounts = this.findPreference("settingsAccounts")!!
    this.settingsBuild = this.findPreference("settingsBuild")!!
    this.settingsDebug = this.findPreference("settingsDebug")!!
    this.settingsEULA = this.findPreference("settingsEULA")!!
    this.settingsFaq = this.findPreference("settingsFaq")!!
    this.settingsLicense = this.findPreference("settingsLicense")!!
    this.settingsVersion = this.findPreference("settingsVersion")!!
    this.settingsVersionCore = this.findPreference("settingsVersionCore")!!

    this.configureAbout(this.settingsAbout)
    this.configureAcknowledgements(this.settingsAcknowledgements)
    this.configureAccounts(this.settingsAccounts)
    this.configureBuild(this.settingsBuild)
    this.configureDebug(this.settingsDebug)
    this.configureEULA(this.settingsEULA)
    this.configureFaq(this.settingsFaq)
    this.configureLicense(this.settingsLicense)
    this.configureVersion(this.settingsVersion)
    this.configureVersionCore(this.settingsVersionCore)
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar()
  }

  private fun configureToolbar() {
    this.supportActionBar?.apply {
      title = getString(R.string.settings)
      subtitle = null
    }
  }

  private fun configureAcknowledgements(preference: Preference) {
    preference.isEnabled = this.viewModel.documents.acknowledgements != null
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsAcknowledgements()
        true
      }
  }

  private fun configureVersion(preference: Preference) {
    preference.setSummaryProvider { this.viewModel.appVersion }
  }

  private fun configureVersionCore(preference: Preference) {
    preference.setSummaryProvider { this.viewModel.buildConfig.simplifiedVersion }

    // Hide the Core version if it's similar to the app version
    preference.isVisible =
      !this.viewModel.appVersion.startsWith(this.viewModel.buildConfig.simplifiedVersion)
  }

  private fun configureBuild(preference: Preference) {
    preference.setSummaryProvider { this.viewModel.buildConfig.vcsCommit }

    if (!this.viewModel.showDebugSettings) {
      preference.setOnPreferenceClickListener {
        this.onTapToDebugSettings(it)
        true
      }
    }
  }

  private fun configureDebug(preference: Preference) {
    preference.setOnPreferenceClickListener {
      this.findNavigationController().openSettingsVersion()
      true
    }

    // Show the debug settings menu, if enabled
    preference.isVisible = this.viewModel.showDebugSettings
  }

  private fun configureLicense(preference: Preference) {
    preference.isEnabled = this.viewModel.documents.licenses != null
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsLicense()
        true
      }
  }

  private fun configureFaq(preference: Preference) {
    preference.isEnabled = false
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsFaq()
        true
      }
  }

  private fun configureEULA(preference: Preference) {
    preference.isEnabled = this.viewModel.documents.eula != null
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsEULA()
        true
      }
  }

  private fun configureAccounts(preference: Preference) {
    if (this.viewModel.buildConfig.allowAccountsAccess) {
      preference.isEnabled = true
      preference.onPreferenceClickListener =
        Preference.OnPreferenceClickListener {
          this.findNavigationController().openSettingsAccounts()
          true
        }
    } else {
      preference.isVisible = false
      preference.isEnabled = false
    }
  }

  private fun configureAbout(preference: Preference) {
    preference.isEnabled = this.viewModel.documents.about != null
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsAbout()
        true
      }
  }

  private fun onTapToDebugSettings(preference: Preference) {
    val context = this.context ?: return

    if (this.tapToDebugSettings == 0) {
      this.viewModel.showDebugSettings = true
      this.settingsDebug.isVisible = true

      // Cancel the toast
      this.toast?.cancel()

      // Reveal the preference, if hidden
      this.listView.run {
        smoothScrollToPosition(adapter!!.itemCount)
      }

      // Unset our click listener
      preference.onPreferenceClickListener = null
    } else {
      if (this.tapToDebugSettings < 6) {
        val message =
          context.getString(R.string.settingsTapToDebug, this.tapToDebugSettings)

        val toast =
          this.toast ?: Toast.makeText(context, message, Toast.LENGTH_LONG)

        this.toast = toast.apply {
          this.setText(message)
          if (!this.view.isShown) {
            this.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL, 0, 0)
            this.show()
          }
        }
      }
      this.tapToDebugSettings -= 1
    }
  }

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }
}
