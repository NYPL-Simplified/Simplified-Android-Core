package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.ReaderBookmark

/**
 * The current state of a particular bookmark.
 */

data class ReaderBookmarkState(
  val account: AccountID,
  val bookmark: ReaderBookmark,
  val localState: ReaderBookmarkLocalState,
  val remoteState: ReaderBookmarkRemoteState)