package org.nypl.simplified.app.settings

import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.io7m.jfunctional.Some
import com.tenmiles.helpstack.HSHelpStack
import com.tenmiles.helpstack.gears.HSDeskGear
import org.nypl.simplified.app.helpstack.HelpstackType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.WebViewActivity
import org.nypl.simplified.books.synced_document.SyncedDocumentType

/**
 * The main settings fragment.
 */

class SettingsFragmentMain : PreferenceFragmentCompat() {

  private lateinit var listener: SettingsFragmentListenerType

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    this.setPreferencesFromResource(R.xml.preferences, rootKey)

    val context = this.requireContext()
    if (context is SettingsFragmentListenerType) {
      this.listener = context
    } else {
      throw IllegalStateException(
        "The context hosting this fragment must implement ${SettingsFragmentListenerType::class.java}")
    }

    val accounts =
      this.findPreference(this.getString(R.string.settings_accounts))
    val about =
      this.findPreference(this.getString(R.string.settings_about))
    val eula =
      this.findPreference(this.getString(R.string.settings_eula))
    val faq =
      this.findPreference(this.getString(R.string.help))
    val licenses =
      this.findPreference(this.getString(R.string.settings_licence_software))

    this.configureAccounts(accounts)
    this.configureAbout(about)
    this.configureEULA(eula)
    this.configureFAQ(faq)
    this.configureLicenses(licenses)
  }

  private fun configureAccounts(accounts: Preference) {
    accounts.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      this.listener.openAccounts()
      true
    }
  }

  private fun configureLicenses(licenses: Preference) {
    val docLicensesOpt = this.listener.documents().licenses
    if (docLicensesOpt is Some<SyncedDocumentType>) {
      val docLicenses = docLicensesOpt.get()
      licenses.intent = this.showLicense(docLicenses)
    } else {
      licenses.isEnabled = false
      licenses.isVisible = false
    }
  }

  private fun configureFAQ(faq: Preference) {
    val helpStackOpt = this.listener.helpstack()
    if (helpStackOpt is Some<HelpstackType>) {
      val helpStack = helpStackOpt.get()
      faq.intent = null
      faq.setOnPreferenceClickListener {
        val stack = HSHelpStack.getInstance(this.activity)

        // XXX: Why is this token hardcoded?
        val gear = HSDeskGear("https://nypl.desk.com/", "4GBRmMv8ZKG8fGehhA", "12060")
        stack.gear = gear
        stack.showHelp(this.activity)
        false
      }
    } else {
      faq.isVisible = false
      faq.isEnabled = false
    }
  }

  private fun configureEULA(eula: Preference) {
    eula.intent = this.webViewIntent(
      uri = "http://www.librarysimplified.org/EULA.html",
      title = this.resources.getString(R.string.settings_eula))
  }

  private fun configureAbout(about: Preference) {
    about.intent = this.webViewIntent(
      uri = "http://www.librarysimplified.org/acknowledgments.html",
      title = this.resources.getString(R.string.settings_about))
  }

  private fun showLicense(docLicenses: SyncedDocumentType): Intent {
    return this.webViewIntent(
      uri = docLicenses.documentGetReadableURL().toString(),
      title = this.resources.getString(R.string.settings_licence_software))
  }

  private fun webViewIntent(uri: String, title: String): Intent {
    val intent = Intent(this.activity, WebViewActivity::class.java)
    val intentBundle = Bundle()
    WebViewActivity.setActivityArguments(intentBundle, uri, title)
    intent.putExtras(intentBundle)
    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    return intent
  }
}