package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.ReaderBookmark

/**
 * The type of events published by the bookmark controller.
 */

sealed class ReaderBookmarkEvent {

  data class ReaderBookmarkSyncStarted(
    val accountID: AccountID)
    : ReaderBookmarkEvent()

  data class ReaderBookmarkSyncFinished(
    val accountID: AccountID)
    : ReaderBookmarkEvent()

  data class ReaderBookmarkSaved(
    val accountID: AccountID,
    val bookmark: ReaderBookmark)
    : ReaderBookmarkEvent()
}
