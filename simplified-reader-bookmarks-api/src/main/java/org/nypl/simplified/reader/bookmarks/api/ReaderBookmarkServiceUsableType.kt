package org.nypl.simplified.reader.bookmarks.api

import com.google.common.util.concurrent.FluentFuture
import io.reactivex.Observable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.Bookmark

/**
 * The "usable" reader bookmark service interface. Usable, in this sense, refers to the
 * fact that clients are able to use the service but aren't able to shut it down.
 */

interface ReaderBookmarkServiceUsableType {

  /**
   * An observable that publishes events about bookmarks.
   */

  val bookmarkEvents: Observable<ReaderBookmarkEvent>

  /**
   * Fetch the bookmark sync status for the given account.
   */

  fun bookmarkSyncStatus(
    accountID: AccountID
  ): ReaderBookmarkSyncEnableStatus

  /**
   * Enable/disable bookmark syncing on the server.
   */

  fun bookmarkSyncEnable(
    accountID: AccountID,
    enabled: Boolean
  ): FluentFuture<ReaderBookmarkSyncEnableResult>

  /**
   * The user wants their current bookmarks.
   */

  fun bookmarkLoad(
    accountID: AccountID,
    book: BookID
  ): FluentFuture<ReaderBookmarks>

  /**
   * The user has created a bookmark.
   */

  fun bookmarkCreate(
    accountID: AccountID,
    bookmark: Bookmark
  ): FluentFuture<Unit>

  /**
   * The user has requested that a bookmark be deleted.
   */

  fun bookmarkDelete(
    accountID: AccountID,
    bookmark: Bookmark
  ): FluentFuture<Unit>
}
