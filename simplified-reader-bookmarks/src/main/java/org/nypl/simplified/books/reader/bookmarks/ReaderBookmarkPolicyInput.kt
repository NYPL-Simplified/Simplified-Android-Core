package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Bookmark

/**
 * The type of input to the bookmark policy.
 */

sealed class ReaderBookmarkPolicyInput {

  /**
   * The type of events presented to the bookmark policy.
   */

  sealed class Event : ReaderBookmarkPolicyInput() {

    sealed class Local : Event() {

      /**
       * An account was deleted.
       */

      data class AccountDeleted(
        val accountID: AccountID)
        : Local()

      /**
       * An account was created.
       */

      data class AccountCreated(
        val account: ReaderBookmarkPolicyAccountState)
        : Local()

      /**
       * An account was updated.
       */

      data class AccountUpdated(
        val account: ReaderBookmarkPolicyAccountState)
        : Local()

      /**
       * A bookmark was created locally.
       */

      data class BookmarkCreated(
        val accountID: AccountID,
        val bookmark: Bookmark)
        : Local()

      /**
       * A bookmark was deleted locally.
       */

      data class BookmarkDeleteRequested(
        val accountID: AccountID,
        val bookmark: Bookmark)
        : Local()

    }

    sealed class Remote : Event() {

      /**
       * Syncing was enabled/disabled on the server.
       */

      data class SyncingEnabled(
        val accountID: AccountID,
        val enabled: Boolean)
        : Remote()

      /**
       * A bookmark was saved on a remote server.
       */

      data class BookmarkSaved(
        val accountID: AccountID,
        val bookmark: Bookmark)
        : Remote()

      /**
       * A bookmark was received from a remote server.
       */

      data class BookmarkReceived(
        val accountID: AccountID,
        val bookmark: Bookmark)
        : Remote()

    }
  }

}