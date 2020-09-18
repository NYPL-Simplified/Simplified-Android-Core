package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookmarkID
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command.LocallySaveBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command.RemotelyDeleteBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command.RemotelyFetchBookmarks
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command.RemotelySendBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Event.LocalBookmarkAlreadyExists

data class ReaderBookmarkPolicyEvaluation<T>(
  val result: T,
  val newState: ReaderBookmarkPolicyState,
  val outputs: List<ReaderBookmarkPolicyOutput>
)

/**
 * A bookmark policy.
 *
 * This policy structure forms a combined State<S, A> and Writer<W, A> monad with S fixed
 * to `ReaderBookmarkPolicyState` and W fixed to `List<ReaderBookmarkPolicyOutput>`.
 */

data class ReaderBookmarkPolicy<T>(
  val f: (ReaderBookmarkPolicyState) -> ReaderBookmarkPolicyEvaluation<T>
) {

  companion object {

    fun <A> evaluatePolicy(
      m: ReaderBookmarkPolicy<A>,
      x: ReaderBookmarkPolicyState
    ): ReaderBookmarkPolicyEvaluation<A> {
      return m.f.invoke(x)
    }

    /**
     * The monadic `return` function. Called `unit` here because `return` is a keyword in Kotlin.
     */

    fun <A> unit(x: A): ReaderBookmarkPolicy<A> {
      return ReaderBookmarkPolicy { state -> ReaderBookmarkPolicyEvaluation(x, state, listOf()) }
    }

    /**
     * A computation that applies `f` to the result of evaluating `m`.
     */

    fun <A, B> map(m: ReaderBookmarkPolicy<A>, f: (A) -> B): ReaderBookmarkPolicy<B> {
      return ReaderBookmarkPolicy { s ->
        val evaluated = evaluatePolicy(m, s)
        ReaderBookmarkPolicyEvaluation(
          result = f.invoke(evaluated.result),
          newState = evaluated.newState,
          outputs = evaluated.outputs
        )
      }
    }

    /**
     * The monadic `bind` function. Called `flatMap` to match Java and Kotlin conventions.
     */

    fun <A, B> flatMap(m: ReaderBookmarkPolicy<A>, f: (A) -> ReaderBookmarkPolicy<B>): ReaderBookmarkPolicy<B> {
      return ReaderBookmarkPolicy { s ->
        val evaluated0 = evaluatePolicy(m, s)
        val next = f.invoke(evaluated0.result)
        val evaluated1 = evaluatePolicy(next, evaluated0.newState)
        evaluated1.copy(outputs = evaluated0.outputs.plus(evaluated1.outputs))
      }
    }

    /**
     * The monadic `then` function.
     */

    fun <A, B> then(m: ReaderBookmarkPolicy<A>, n: ReaderBookmarkPolicy<B>): ReaderBookmarkPolicy<B> {
      return flatMap(m) { n }
    }

    /**
     * Evaluate all of the given computations in list order and return a computation that yields
     * a list of the results.
     */

    fun <A> sequence(ms: List<ReaderBookmarkPolicy<A>>): ReaderBookmarkPolicy<List<A>> {
      return ms.foldRight(unit(listOf())) { op, accumulated ->
        accumulated.flatMap { accumulatedResults ->
          op.flatMap { result ->
            unit(accumulatedResults.plus(result))
          }
        }
      }
    }

    /**
     * Evaluate all of the given computations in list order and discard the results.
     */

    fun <A> sequenceDiscarding(ms: List<ReaderBookmarkPolicy<A>>): ReaderBookmarkPolicy<Unit> {
      return ms.foldRight(unit(Unit), Companion::then)
    }

    fun putState(newState: ReaderBookmarkPolicyState): ReaderBookmarkPolicy<Unit> {
      return ReaderBookmarkPolicy {
        ReaderBookmarkPolicyEvaluation(result = Unit, newState = newState, outputs = listOf())
      }
    }

    fun getState(): ReaderBookmarkPolicy<ReaderBookmarkPolicyState> {
      return ReaderBookmarkPolicy { state ->
        ReaderBookmarkPolicyEvaluation(result = state, newState = state, outputs = listOf())
      }
    }

    fun updateState(f: (ReaderBookmarkPolicyState) -> ReaderBookmarkPolicyState): ReaderBookmarkPolicy<Unit> {
      return getState().flatMap { state -> putState(f.invoke(state)) }
    }

    fun getBookmarksState(
      account: AccountID
    ): ReaderBookmarkPolicy<Map<BookmarkID, ReaderBookmarkState>> {
      return getState().map { state -> state.bookmarksByAccount[account] ?: mapOf() }
    }

    fun getBookmarkState(
      account: AccountID,
      id: BookmarkID
    ): ReaderBookmarkPolicy<ReaderBookmarkState?> {
      return getBookmarksState(account).map { bookmarks -> bookmarks[id] }
    }

    fun getAccountsState(): ReaderBookmarkPolicy<Map<AccountID, ReaderBookmarkPolicyAccountState>> {
      return getState().map { state -> state.accountState }
    }

    fun getAccountState(id: AccountID): ReaderBookmarkPolicy<ReaderBookmarkPolicyAccountState?> {
      return getAccountsState().map { accounts -> accounts[id] }
    }

    fun emitOutputs(outputs: List<ReaderBookmarkPolicyOutput>): ReaderBookmarkPolicy<Unit> {
      return ReaderBookmarkPolicy { state ->
        ReaderBookmarkPolicyEvaluation(result = Unit, newState = state, outputs = outputs)
      }
    }

    fun emitOutput(output: ReaderBookmarkPolicyOutput): ReaderBookmarkPolicy<Unit> {
      return emitOutputs(listOf(output))
    }

    fun doNothing(): ReaderBookmarkPolicy<Unit> {
      return ReaderBookmarkPolicy { state ->
        ReaderBookmarkPolicyEvaluation(result = Unit, newState = state, outputs = listOf())
      }
    }

    fun removeAccount(account: AccountID): ReaderBookmarkPolicy<Unit> {
      return updateState { state -> state.copy(accountState = state.accountState.minus(account)) }
    }

    fun updateAccount(account: ReaderBookmarkPolicyAccountState): ReaderBookmarkPolicy<Unit> {
      return updateState { state ->
        state.copy(accountState = state.accountState.plus(Pair(account.accountID, account)))
      }
    }

    fun updateBookmark(bookmarkState: ReaderBookmarkState): ReaderBookmarkPolicy<Unit> {
      return updateState { state ->
        val accountBookmarksInitial =
          state.bookmarksByAccount.get(bookmarkState.account) ?: mapOf()
        val accountBookmarksUpdate =
          accountBookmarksInitial.plus(Pair(bookmarkState.bookmark.bookmarkId, bookmarkState))
        val bookmarksUpdate =
          state.bookmarksByAccount.plus(Pair(bookmarkState.account, accountBookmarksUpdate))
        state.copy(bookmarksByAccount = bookmarksUpdate)
      }
    }

    private fun remoteDeleteBookmark(
      accountID: AccountID,
      bookmark: ReaderBookmarkState
    ): ReaderBookmarkPolicy<Unit> {
      return emitOutput(RemotelyDeleteBookmark(accountID, bookmark.bookmark))
    }

    private fun remoteDeleteBookmarkIfPossible(
      accountID: AccountID,
      bookmark: ReaderBookmarkState
    ): ReaderBookmarkPolicy<Unit> {
      return getAccountState(accountID)
        .andThen { account ->
          if (account != null && account.canSync) {
            remoteDeleteBookmark(accountID, bookmark)
          } else {
            doNothing()
          }
        }
    }

    private fun remoteSendAllUnsentBookmarksIfPossible(
      account: AccountID
    ): ReaderBookmarkPolicy<Unit> {
      return getAccountState(account)
        .andThen { accountState ->
          if (accountState != null && accountState.canSync) {
            getBookmarksState(account)
              .flatMap { bookmarks -> remoteSendAllUnsentBookmarks(account, bookmarks) }
          } else {
            doNothing()
          }
        }
    }

    private fun remoteSendAllUnsentBookmarks(
      accountID: AccountID,
      bookmarksState: Map<BookmarkID, ReaderBookmarkState>
    ): ReaderBookmarkPolicy<Unit> {
      return sequenceDiscarding(
        bookmarksThatRequireSyncingInAccount(accountID, bookmarksState)
          .map { bookmark -> emitOutput(RemotelySendBookmark(accountID, bookmark.bookmark)) }
      )
    }

    private fun remoteFetchAllBookmarks(account: AccountID): ReaderBookmarkPolicy<Unit> {
      return emitOutput(RemotelyFetchBookmarks(account))
    }

    private fun remoteSyncAllIfPossible(account: AccountID): ReaderBookmarkPolicy<Unit> {
      return getAccountState(account)
        .andThen { accountState ->
          if (accountState != null && accountState.canSync) {
            getBookmarksState(account)
              .flatMap { bookmarks -> remoteSyncAll(account, bookmarks) }
          } else {
            doNothing()
          }
        }
    }

    private fun remoteSyncAll(
      account: AccountID,
      bookmarks: Map<BookmarkID, ReaderBookmarkState>
    ): ReaderBookmarkPolicy<Unit> {
      return remoteSendAllUnsentBookmarks(account, bookmarks)
        .andThen { remoteFetchAllBookmarks(account) }
    }

    private fun bookmarksThatRequireSyncingInAccount(
      account: AccountID,
      bookmarks: Map<BookmarkID, ReaderBookmarkState>
    ): List<ReaderBookmarkState> {
      return bookmarks.filterValues { bookmark -> bookmarkRequiresSyncing(account, bookmark) }
        .values
        .toList()
    }

    private fun bookmarkRequiresSyncing(
      account: AccountID,
      bookmark: ReaderBookmarkState
    ): Boolean {
      return if (bookmark.account == account) {
        when (bookmark.remoteState) {
          is ReaderBookmarkRemoteState.Sending -> false
          is ReaderBookmarkRemoteState.Unknown -> true
          is ReaderBookmarkRemoteState.Saved -> false
          is ReaderBookmarkRemoteState.Deleted -> false
          is ReaderBookmarkRemoteState.Deleting -> false
        }
      } else {
        false
      }
    }

    fun evaluateInput(input: ReaderBookmarkPolicyInput): ReaderBookmarkPolicy<Unit> {
      return when (input) {
        is Event.Local.BookmarkCreated ->
          onEventBookmarkLocalCreated(input)
        is Event.Local.BookmarkDeleteRequested ->
          onEventBookmarkLocalDeleted(input)
        is Event.Remote.BookmarkReceived ->
          onEventBookmarkRemoteReceived(input)
        is Event.Remote.BookmarkSaved ->
          onEventBookmarkRemoteSaved(input)
        is Event.Local.AccountDeleted ->
          onEventAccountDeleted(input)
        is Event.Local.AccountCreated ->
          onEventAccountCreated(input)
        is Event.Remote.SyncingEnabled ->
          onEventSyncingEnabled(input)
        is Event.Local.AccountUpdated ->
          onEventAccountUpdated(input)
        is Event.Local.AccountLoggedIn ->
          onEventAccountLoggedIn(input)
      }
    }

    private fun onEventBookmarkLocalCreated(
      event: Event.Local.BookmarkCreated
    ): ReaderBookmarkPolicy<Unit> {
      return getBookmarkState(event.accountID, event.bookmark.bookmarkId).flatMap { bookmarkState ->
        if (bookmarkState != null) {
          when (bookmarkState.localState) {
            /*
             * If the bookmark was previously deleted, then recreate it and send it to the
             * server (if possible).
             */

            ReaderBookmarkLocalState.Deleted -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  account = event.accountID,
                  bookmark = event.bookmark,
                  localState = ReaderBookmarkLocalState.Saved,
                  remoteState = ReaderBookmarkRemoteState.Unknown
                )

              updateBookmark(newBookmarkState)
                .andThen { remoteSendAllUnsentBookmarksIfPossible(event.accountID) }
            }

            /*
             * If the bookmark is already locally saved, then ignore it.
             */

            ReaderBookmarkLocalState.Saved ->
              emitOutput(LocalBookmarkAlreadyExists(event.accountID, event.bookmark))
          }
        } else {
          /*
           * If nothing is known about the bookmark, then save it locally and then sync
           * with the server (if possible).
           */

          val newBookmarkState =
            ReaderBookmarkState(
              account = event.accountID,
              bookmark = event.bookmark,
              localState = ReaderBookmarkLocalState.Saved,
              remoteState = ReaderBookmarkRemoteState.Unknown
            )

          updateBookmark(newBookmarkState)
            .andThen { emitOutput(LocallySaveBookmark(event.accountID, event.bookmark)) }
            .andThen { remoteSendAllUnsentBookmarksIfPossible(event.accountID) }
        }
      }
    }

    private fun onEventBookmarkLocalDeleted(
      event: Event.Local.BookmarkDeleteRequested
    ): ReaderBookmarkPolicy<Unit> {
      return getBookmarkState(event.accountID, event.bookmark.bookmarkId).flatMap { bookmarkState ->
        if (bookmarkState != null) {
          when (bookmarkState.localState) {
            /*
             * If the bookmark is already deleted, then ignore it.
             */

            ReaderBookmarkLocalState.Deleted ->
              doNothing()

            /*
             * If the bookmark exists, then delete it and tell the server to delete it too.
             */

            ReaderBookmarkLocalState.Saved -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  account = event.accountID,
                  bookmark = event.bookmark,
                  localState = ReaderBookmarkLocalState.Deleted,
                  remoteState = ReaderBookmarkRemoteState.Deleting
                )

              updateBookmark(newBookmarkState)
                .andThen { remoteDeleteBookmarkIfPossible(event.accountID, newBookmarkState) }
            }
          }
        } else {
          /*
           * If the bookmark isn't known, then ignore it.
           */

          doNothing()
        }
      }
    }

    private fun onEventBookmarkRemoteReceived(
      event: Event.Remote.BookmarkReceived
    ): ReaderBookmarkPolicy<Unit> {
      return getBookmarkState(event.accountID, event.bookmark.bookmarkId).flatMap { bookmarkState ->
        if (bookmarkState != null) {
          when (bookmarkState.localState) {
            /*
             * If the bookmark has been deleted locally, but the server has sent it to us, then
             * tell the server we want it deleted.
             */

            ReaderBookmarkLocalState.Deleted -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  account = event.accountID,
                  bookmark = event.bookmark,
                  localState = ReaderBookmarkLocalState.Deleted,
                  remoteState = ReaderBookmarkRemoteState.Deleting
                )

              updateBookmark(newBookmarkState)
                .andThen { remoteDeleteBookmarkIfPossible(event.accountID, newBookmarkState) }
            }

            /*
             * If the bookmark has been saved locally, ignore it.
             */

            ReaderBookmarkLocalState.Saved -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  account = event.accountID,
                  bookmark = event.bookmark,
                  localState = ReaderBookmarkLocalState.Saved,
                  remoteState = ReaderBookmarkRemoteState.Saved
                )

              updateBookmark(newBookmarkState)
                .andThen { emitOutput(LocalBookmarkAlreadyExists(event.accountID, event.bookmark)) }
            }
          }
        } else {
          /*
           * If nothing is known about the bookmark locally, then save it to disk.
           */

          val newBookmarkState =
            ReaderBookmarkState(
              account = event.accountID,
              bookmark = event.bookmark,
              localState = ReaderBookmarkLocalState.Saved,
              remoteState = ReaderBookmarkRemoteState.Saved
            )

          updateBookmark(newBookmarkState)
            .andThen { emitOutput(LocallySaveBookmark(event.accountID, event.bookmark)) }
        }
      }
    }

    private fun onEventBookmarkRemoteSaved(
      event: Event.Remote.BookmarkSaved
    ): ReaderBookmarkPolicy<Unit> {
      return getBookmarkState(event.accountID, event.bookmark.bookmarkId).flatMap { bookmarkState ->
        if (bookmarkState != null) {
          /*
           * If the bookmark is known, then mark it as having been saved remotely.
           */

          updateBookmark(bookmarkState.copy(remoteState = ReaderBookmarkRemoteState.Saved))
        } else {
          /*
           * If nothing is known about the bookmark locally, then ignore it.
           */

          doNothing()
        }
      }
    }

    private fun onEventAccountDeleted(event: Event.Local.AccountDeleted): ReaderBookmarkPolicy<Unit> {
      return removeAccount(event.accountID)
    }

    private fun onEventAccountCreated(event: Event.Local.AccountCreated): ReaderBookmarkPolicy<Unit> {
      return updateAccount(event.account)
        .andThen { remoteSyncAllIfPossible(event.account.accountID) }
    }

    private fun onEventSyncingEnabled(event: Event.Remote.SyncingEnabled): ReaderBookmarkPolicy<Unit> {
      return getAccountState(event.accountID).flatMap { accountState ->
        if (accountState != null) {
          val syncPrev = accountState.canSync
          val newAccountState = accountState.copy(syncEnabledOnServer = event.enabled)
          val syncCurrent = newAccountState.canSync

          /*
           * Update the account, and then decide whether or not to try syncing bookmarks. If
           * syncing was not previously possible and it suddenly becomes possible, treat this
           * as an opportunity to run the sync.
           */

          updateAccount(newAccountState)
            .andThen {
              if (!syncPrev && syncCurrent) {
                remoteFetchAllBookmarks(event.accountID)
              } else {
                doNothing()
              }
            }
        } else {
          doNothing()
        }
      }
    }

    private fun onEventAccountUpdated(event: Event.Local.AccountUpdated): ReaderBookmarkPolicy<Unit> {
      return updateAccount(event.account)
        .andThen { remoteSyncAllIfPossible(event.account.accountID) }
    }

    private fun onEventAccountLoggedIn(event: Event.Local.AccountLoggedIn): ReaderBookmarkPolicy<Unit> {
      return updateAccount(event.account)
        .andThen { remoteSyncAllIfPossible(event.account.accountID) }
    }
  }

  fun <B> andThen(f: (T) -> ReaderBookmarkPolicy<B>): ReaderBookmarkPolicy<B> =
    flatMap(this, f)

  fun <B> flatMap(f: (T) -> ReaderBookmarkPolicy<B>): ReaderBookmarkPolicy<B> =
    flatMap(this, f)

  fun <B> map(f: (T) -> B): ReaderBookmarkPolicy<B> =
    map(this, f)
}
