package org.nypl.simplified.accounts.api

import java.net.URI

/**
 * Preferences for a specific account.
 */

data class AccountPreferences(

  /**
   * `true` if the user has permitted bookmarks to be synced to the server for the current account.
   */

  val bookmarkSyncingPermitted: Boolean,

  /**
   * An override for the catalog URI. This is used for for custom OPDS feeds.
   */

  val catalogURIOverride: URI?
) {

  companion object {

    /**
     * @return A set of default preferences
     */

    fun defaultPreferences(): AccountPreferences {
      return AccountPreferences(
        bookmarkSyncingPermitted = false,
        catalogURIOverride = null
      )
    }
  }
}
