package org.nypl.simplified.app.settings

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.app.NavigationDrawerActivity
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.helpstack.HelpstackType
import org.nypl.simplified.books.document_store.DocumentStoreType

/**
 * The main activity used to display the settings section.
 */

class SettingsActivity : NavigationDrawerActivity(), SettingsFragmentListenerType {

  override fun openVersion(developerOptions: Boolean) {
    val intent = Intent()
    intent.setClass(this, SettingsVersionActivity::class.java)
    this.startActivity(intent)
  }

  override fun helpstack(): OptionType<HelpstackType> =
    Simplified.getHelpStack()

  override fun documents(): DocumentStoreType =
    Simplified.getDocumentStore()

  override fun navigationDrawerShouldShowIndicator(): Boolean =
    true

  override fun navigationDrawerGetActivityTitle(resources: Resources): String =
    resources.getString(R.string.settings)

  private lateinit var settingsFragment: SettingsFragmentMain

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    this.settingsFragment = SettingsFragmentMain()
  }

  override fun openAccounts() {
    val intent = Intent()
    intent.setClass(this, SettingsAccountsActivity::class.java)
    this.startActivity(intent)
  }

  override fun onStart() {
    super.onStart()

    this.supportFragmentManager
      .beginTransaction()
      .replace(R.id.content_frame, this.settingsFragment)
      .addToBackStack(null)
      .commit()
  }
}