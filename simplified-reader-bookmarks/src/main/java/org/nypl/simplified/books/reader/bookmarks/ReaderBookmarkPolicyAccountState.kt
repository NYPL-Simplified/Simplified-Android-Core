package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.accounts.api.AccountID

/**
 * The state of an account with regards to bookmark syncing.
 */

data class ReaderBookmarkPolicyAccountState(
  val accountID: AccountID,

  /**
   * `true` if syncing is supported by the provider of the account.
   */

  val syncSupportedByAccount: Boolean,

  /**
   * `true` if syncing is enabled on the remote server.
   */

  val syncEnabledOnServer: Boolean,

  /**
   * `true` if the user permits bookmark syncing on this device.
   */

  val syncPermittedByUser: Boolean
) {

  val canSync =
    this.syncSupportedByAccount && this.syncEnabledOnServer && this.syncPermittedByUser
}
