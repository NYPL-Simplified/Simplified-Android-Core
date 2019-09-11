package org.nypl.simplified.app.settings

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
