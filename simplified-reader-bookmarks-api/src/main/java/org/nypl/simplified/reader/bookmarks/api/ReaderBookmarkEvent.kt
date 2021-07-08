package org.nypl.simplified.reader.bookmarks.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Bookmark

/**
 * The type of events published by the bookmark controller.
 */

sealed class ReaderBookmarkEvent {

  /**
   * The status of bookmark syncing changed.
   */

  data class ReaderBookmarkSyncSettingChanged(
    val accountID: AccountID,
    val status: ReaderBookmarkSyncEnableStatus
  ) : ReaderBookmarkEvent()

  /**
   * Synchronizing bookmarks for the given account has started.
   */

  data class ReaderBookmarkSyncStarted(
    val accountID: AccountID
  ) : ReaderBookmarkEvent()

  /**
   * Synchronizing bookmarks for the given account has finished.
   */

  data class ReaderBookmarkSyncFinished(
    val accountID: AccountID
  ) : ReaderBookmarkEvent()

  /**
   * A bookmark was saved for the given account.
   */

  data class ReaderBookmarkSaved(
    val accountID: AccountID,
    val bookmark: Bookmark
  ) : ReaderBookmarkEvent()
}
