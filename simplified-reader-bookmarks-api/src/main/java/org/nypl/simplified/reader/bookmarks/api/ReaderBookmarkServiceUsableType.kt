package org.nypl.simplified.reader.bookmarks.api

import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.observable.ObservableReadableType

/**
 * The "usable" reader bookmark service interface. Usable, in this sense, refers to the
 * fact that clients are able to use the service but aren't able to shut it down.
 */

interface ReaderBookmarkServiceUsableType {

  /**
   * An observable that publishes events about bookmarks.
   */

  val bookmarkEvents: ObservableReadableType<ReaderBookmarkEvent>

  /**
   * The user wants their current bookmarks.
   */

  fun bookmarkLoad(
    accountID: AccountID,
    book: BookID): FluentFuture<ReaderBookmarks>

  /**
   * The user has created a bookmark.
   */

  fun bookmarkCreate(
    accountID: AccountID,
    bookmark: Bookmark): FluentFuture<Unit>

  /**
   * The user has requested that a bookmark be deleted.
   */

  fun bookmarkDelete(
    accountID: AccountID,
    bookmark: Bookmark): FluentFuture<Unit>
}