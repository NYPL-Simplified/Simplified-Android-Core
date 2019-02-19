package org.nypl.simplified.books.reader.bookmarks

/**
 * The states in which a local bookmark can be.
 */

sealed class ReaderBookmarkLocalState {
  object Deleted : ReaderBookmarkLocalState()
  object Saved : ReaderBookmarkLocalState()
}