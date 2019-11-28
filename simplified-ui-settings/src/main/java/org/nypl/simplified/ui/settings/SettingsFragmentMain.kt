package org.nypl.simplified.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.io7m.jfunctional.Some
import org.librarysimplified.services.api.Services
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.ui.toolbar.ToolbarHostType

/**
 * The main settings page containing links to other settings pages.
 */

class SettingsFragmentMain : PreferenceFragmentCompat() {

  private lateinit var documents: DocumentStoreType

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    this.setPreferencesFromResource(R.xml.settings, rootKey)

    val services = Services.serviceDirectory()
    this.documents = services.requireService(DocumentStoreType::class.java)

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

  override fun onStart() {
    super.onStart()
    this.configureToolbar()
  }

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.settings),
        subtitle = ""
      )
      host.toolbarSetBackArrowConditionally(
        context = host,
        shouldArrowBePresent = {
          this.findNavigationController().backStackSize() > 1
        },
        onArrowClicked = {
          this.findNavigationController().popBackStack()
        })
    } else {
      throw IllegalStateException("The activity ($host) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }
  }

  private fun configureAcknowledgements(preference: Preference) {
    preference.isEnabled = this.documents.acknowledgements is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsAcknowledgements()
        true
      }
  }

  private fun configureVersion(preference: Preference) {
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsVersion()
        true
      }
  }

  private fun configureLicense(preference: Preference) {
    preference.isEnabled = this.documents.licenses is Some
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
    preference.isEnabled = this.documents.eula is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsEULA()
        true
      }
  }

  private fun configureAccounts(preference: Preference) {
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsAccounts()
        true
      }
  }

  private fun configureAbout(preference: Preference) {
    preference.isEnabled = this.documents.about is Some
    preference.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        this.findNavigationController().openSettingsAbout()
        true
      }
  }

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }
}
