package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Bookmark

/**
 * The current state of a particular bookmark.
 */

data class ReaderBookmarkState(
  val account: AccountID,
  val bookmark: Bookmark,
  val localState: ReaderBookmarkLocalState,
  val remoteState: ReaderBookmarkRemoteState
)
