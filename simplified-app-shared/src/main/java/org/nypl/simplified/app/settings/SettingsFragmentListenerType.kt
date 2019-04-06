package org.nypl.simplified.app.settings

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.app.HelpstackType
import org.nypl.simplified.books.document_store.DocumentStoreType

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

}
