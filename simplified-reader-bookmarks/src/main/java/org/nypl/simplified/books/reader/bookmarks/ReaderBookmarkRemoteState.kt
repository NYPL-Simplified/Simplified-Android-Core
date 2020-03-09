package org.nypl.simplified.books.reader.bookmarks

/**
 * The states in which a remote bookmark can be.
 */

sealed class ReaderBookmarkRemoteState {
  object Sending : ReaderBookmarkRemoteState()
  object Deleting : ReaderBookmarkRemoteState()
  object Deleted : ReaderBookmarkRemoteState()
  object Unknown : ReaderBookmarkRemoteState()
  object Saved : ReaderBookmarkRemoteState()
}
