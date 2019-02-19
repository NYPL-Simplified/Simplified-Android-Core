package org.nypl.simplified.books.reader.bookmarks

import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.observable.ObservableReadableType

interface ReaderBookmarkControllerType : AutoCloseable {

  override fun close()

  val bookmarkEvents: ObservableReadableType<ReaderBookmarkEvent>

  /**
   * The user has created a bookmark.
   */

  fun onBookmarkCreated(
    accountID: AccountID,
    bookmark: ReaderBookmark): FluentFuture<Unit>

  /**
   * The user has requested that a bookmark be deleted.
   */

  fun onBookmarkDeleteRequested(
    accountID: AccountID,
    bookmark: ReaderBookmark): FluentFuture<Unit>

}

