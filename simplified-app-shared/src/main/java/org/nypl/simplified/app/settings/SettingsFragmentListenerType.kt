package org.nypl.simplified.app.settings

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.app.helpstack.HelpstackType
import org.nypl.simplified.documents.store.DocumentStoreType

/**
 * The listener interface that must be implemented by activities hosting [SettingsFragmentListenerType].
 */

interface SettingsFragmentListenerType {

  /**
   * Return the document store.
   */

  fun documents(): DocumentStoreType

  /**
   * Return the Helpstack instance, if one exists.
   */

  fun helpstack(): OptionType<HelpstackType>

  /**
   * Open the accounts activity.
   */

  fun openAccounts()

  /**
   * Open the version activity.
   *
   * @param developerOptions `true` If the developer options should be shown
   */

  fun openVersion(developerOptions: Boolean)

}
