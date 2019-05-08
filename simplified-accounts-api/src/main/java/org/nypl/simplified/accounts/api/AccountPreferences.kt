package org.nypl.simplified.accounts.api

/**
 * Preferences for a specific account.
 */

data class AccountPreferences(

  /**
   * `true` if the user has permitted bookmarks to be synced to the server for the current account.
   */

  val bookmarkSyncingPermitted: Boolean) {

  companion object {

    /**
     * @return A set of default preferences
     */

    fun defaultPreferences(): AccountPreferences {
      return AccountPreferences(bookmarkSyncingPermitted = false)
    }
  }
}
