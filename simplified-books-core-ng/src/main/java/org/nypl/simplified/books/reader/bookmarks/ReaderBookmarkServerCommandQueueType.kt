package org.nypl.simplified.books.reader.bookmarks

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListenableFuture
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.ReaderBookmark
import java.net.URI

/**
 * The type of asynchronous bookmark command queues.
 */

interface ReaderBookmarkServerCommandQueueType : AutoCloseable {

  /**
   * Stop the queue. All other methods except for `isRunning` will throw exceptions after
   * this call.
   */

  override fun close()

  /**
   * @return `true` if the queue is running.
   */

  fun isRunning(): Boolean

  /**
   * Send a bookmark to the server.
   */

  fun sendBookmark(
    accountID: AccountID,
    targetURI: URI,
    credentials: AccountAuthenticationCredentials,
    bookmark: ReaderBookmark)
    : FluentFuture<Unit>

  /**
   * Delete a bookmark from the server.
   */

  fun deleteBookmark(
    accountID: AccountID,
    credentials: AccountAuthenticationCredentials,
    bookmark: ReaderBookmark)
    : FluentFuture<Unit>

  /**
   * A bookmark received from the server.
   */

  data class ReceivedBookmark(
    val accountID: AccountID,
    val bookmark: ReaderBookmark)

  /**
   * Receive bookmarks from the server.
   */

  fun receiveBookmarks(
    accountID: AccountID,
    targetURI: URI,
    credentials: AccountAuthenticationCredentials)
    : FluentFuture<List<ReceivedBookmark>>


  /**
   * @return `true` if annotation syncing is enabled for the given account
   */

  fun syncingIsEnabled(
    settingsURI: URI,
    credentials: AccountAuthenticationCredentials)
    : FluentFuture<Boolean>

  /**
   * Enable or disable annotation syncing for the given account.
   */

  fun syncingEnable(
    settingsURI: URI,
    credentials: AccountAuthenticationCredentials,
    enabled: Boolean)
    : FluentFuture<Unit>
}
