package org.nypl.simplified.reader.bookmarks.api

import org.nypl.simplified.accounts.api.AccountID

/**
 * The current status of the bookmark syncing configuration switch. The configuration is either
 * idle, or is in the process of changing.
 */

sealed class ReaderBookmarkSyncEnableStatus {

  /**
   * The switch is idle.
   */

  data class Idle(
    val accountID: AccountID,
    val status: ReaderBookmarkSyncEnableResult
  ) : ReaderBookmarkSyncEnableStatus()

  /**
   * The switch is changing.
   */

  data class Changing(
    val accountID: AccountID
  ) : ReaderBookmarkSyncEnableStatus()
}
