package org.nypl.simplified.ui.settings

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.io7m.jfunctional.Some
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.ui.host.HostViewModel
import org.nypl.simplified.ui.host.HostViewModelReadableType

/**
 * The main settings page containing links to other settings pages.
 */

class SettingsFragmentMain : PreferenceFragmentCompat() {

  private lateinit var documents: DocumentStoreType
  private lateinit var hostModel: HostViewModelReadableType
  private lateinit var navigation: SettingsNavigationControllerType

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    this.setPreferencesFromResource(R.xml.settings, rootKey)

    this.hostModel =
      ViewModelProviders.of(this.requireActivity())
        .get(HostViewModel::class.java)

    this.navigation =
      this.hostModel.navigationController(SettingsNavigationControllerType::class.java)
    this.documents =
      this.hostModel.services.requireService(DocumentStoreType::class.java)

    val settingsAbout =
      this.findPreference<Preference>("settingsAbout")!!
    val settingsAcknowledgements =
      this.findPreference<Preference>("settingsAcknowledgements")!!
    val settingsAccounts =
      this.findPreference<Preference>("settingsAccounts")!!
    val settingsEULA =
      this.findPreference<Preference>("settingsEULA")!!
    val settingsFaq =
      this.findPreference<Preference>("settingsFaq")!!
    val settingsLicense =
      this.findPreference<Preference>("settingsLicense")!!
    val settingsVersion =
      this.findPreference<Preference>("settingsVersion")!!

    this.configureAbout(settingsAbout)
    this.configureAcknowledgements(settingsAcknowledgements)
    this.configureAccounts(settingsAccounts)
    this.configureEULA(settingsEULA)
    this.configureFaq(settingsFaq)
    this.configureLicense(settingsLicense)
    this.configureVersion(settingsVersion)
  }

  private fun configureAcknowledgements(preference: Preference) {
    preference.isEnabled = this.documents.acknowledgements is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsAcknowledgements()
        true
      }
  }

  private fun configureVersion(preference: Preference) {
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsVersion()
        true
      }
  }

  private fun configureLicense(preference: Preference) {
    preference.isEnabled = this.documents.licenses is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsLicense()
        true
      }
  }

  private fun configureFaq(preference: Preference) {
    preference.isEnabled = false
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsFaq()
        true
      }
  }

  private fun configureEULA(preference: Preference) {
    preference.isEnabled = this.documents.eula is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsEULA()
        true
      }
  }

  private fun configureAccounts(preference: Preference) {
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsAccounts()
        true
      }
  }

  private fun configureAbout(preference: Preference) {
    preference.isEnabled = this.documents.about is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.navigation.openSettingsAbout()
        true
      }
  }
}
