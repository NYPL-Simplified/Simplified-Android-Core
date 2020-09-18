package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkID

/**
 * The current (immutable) state of the bookmark policy.
 */

data class ReaderBookmarkPolicyState(
  val accountState: Map<AccountID, ReaderBookmarkPolicyAccountState>,
  val bookmarksByAccount: Map<AccountID, Map<BookmarkID, ReaderBookmarkState>>
) {

  /**
   * The set of all bookmarks by bookmark ID. Note that this value is lossy; there may be
   * bookmarks in different accounts with the same bookmark ID, as bookmark IDs are not guaranteed
   * to be unique across accounts. This property is primarily convenient for unit testing, to
   * check if a bookmark exists or not.
   */

  val bookmarksAll: Lazy<Map<BookmarkID, ReaderBookmarkState>> = lazy {
    this.bookmarksByAccount.values.fold(mapOf<BookmarkID, ReaderBookmarkState>()) {
      accumulated, current ->
      accumulated.plus(current)
    }
  }

  companion object {

    /**
     * Create a new empty bookmark policy state.
     */

    fun empty(): ReaderBookmarkPolicyState {
      return create(
        initialAccounts = setOf(),
        locallySaved = mapOf()
      )
    }

    /**
     * Create a new bookmark policy state.
     *
     * @param initialAccounts The state of the accounts, initially
     * @param locallySaved The set of bookmarks that have been saved locally
     */

    fun create(
      initialAccounts: Set<ReaderBookmarkPolicyAccountState>,
      locallySaved: Map<AccountID, Set<Bookmark>>
    ): ReaderBookmarkPolicyState {
      val states: Map<AccountID, Map<BookmarkID, ReaderBookmarkState>> =
        locallySaved.mapValues { savedEntry ->
          savedEntry.value.map { bookmark ->
            Pair(
              bookmark.bookmarkId,
              ReaderBookmarkState(
                account = savedEntry.key,
                bookmark = bookmark,
                localState = ReaderBookmarkLocalState.Saved,
                remoteState = ReaderBookmarkRemoteState.Unknown
              )
            )
          }.toMap()
        }

      val accounts =
        initialAccounts.associateBy { account -> account.accountID }

      return ReaderBookmarkPolicyState(
        accountState = accounts,
        bookmarksByAccount = states.toMap()
      )
    }
  }
}
