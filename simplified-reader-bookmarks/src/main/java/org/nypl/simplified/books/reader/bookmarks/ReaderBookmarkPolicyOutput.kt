package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Bookmark

/**
 * The type of output produced by the bookmark policy.
 */

sealed class ReaderBookmarkPolicyOutput {

  /**
   * The type of commands produced by the bookmark policy.
   */

  sealed class Command : ReaderBookmarkPolicyOutput() {

    /**
     * The given bookmark should be saved to disk.
     */

    data class LocallySaveBookmark(
      val accountID: AccountID,
      val bookmark: Bookmark)
      : Command()

    /**
     * The given bookmark should be sent to the server.
     */

    data class RemotelySendBookmark(
      val accountID: AccountID,
      val bookmark: Bookmark)
      : Command()

    /**
     * The given bookmark should be deleted from the server.
     */

    data class RemotelyDeleteBookmark(
      val accountID: AccountID,
      val bookmark: Bookmark)
      : Command()

    /**
     * Bookmarks should be fetched from the server for the given account.
     */

    data class RemotelyFetchBookmarks(
      val accountID: AccountID)
      : Command()
  }

  /**
   * The type of events produced by the bookmark engine.
   */

  sealed class Event : ReaderBookmarkPolicyOutput() {

    /**
     * A bookmark already exists.
     */

    data class LocalBookmarkAlreadyExists(
      val accountID: AccountID,
      val bookmark: Bookmark)
      : Event()

  }

}