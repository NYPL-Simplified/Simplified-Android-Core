package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServerCommandQueueType.ReceivedBookmark
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The default implementation of the {@link ReaderBookmarkServerCommandQueueType} interface.
 */

class ReaderBookmarkServerCommandQueue(
  private val httpCalls: ReaderBookmarkHTTPCallsType,
  private val executor: ListeningExecutorService) : ReaderBookmarkServerCommandQueueType {

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      this.executor.shutdown()
    }
  }

  companion object {

    /**
     * Create a new command queue.
     */

    fun create(
      httpCalls: ReaderBookmarkHTTPCallsType,
      executors: () -> ListeningExecutorService): ReaderBookmarkServerCommandQueueType {
      return ReaderBookmarkServerCommandQueue(
        httpCalls = httpCalls,
        executor = executors.invoke())
    }
  }

  private val logger = LoggerFactory.getLogger(ReaderBookmarkServerCommandQueue::class.java)
  private val objectMapper = ObjectMapper()
  private val closed = AtomicBoolean(false)
  private val bookmarkSendRetries = 3
  private val bookmarkSendRetrySleepSeconds = 3L
  private val bookmarkDeleteRetries = 3
  private val bookmarkDeleteRetrySleepSeconds = 3L

  /**
   * A queued command.
   */

  private interface Command<T> {
    fun execute(
      future: SettableFuture<T>,
      queue: ReaderBookmarkServerCommandQueue): T
  }

  /**
   * Send a bookmark to the server.
   */

  data class CommandSendBookmark(
    private val accountID: AccountID,
    private val targetURI: URI,
    private val accountAuthenticationCredentials: AccountAuthenticationCredentials,
    private val bookmark: ReaderBookmark,
    private val retries: Int)
    : Command<Unit> {

    private var retriesLeft = this.retries

    override fun execute(
      future: SettableFuture<Unit>,
      queue: ReaderBookmarkServerCommandQueue) {
      while (true) {
        if (future.isCancelled) {
          throw CancellationException()
        }

        try {
          --this.retriesLeft
          queue.httpCalls.bookmarkAdd(
            this.targetURI,
            this.accountAuthenticationCredentials,
            BookmarkAnnotations.fromBookmark(queue.objectMapper, this.bookmark))
          queue.logger.debug("bookmark {} sent to server successfully", this.bookmark.bookmarkId.value)
          return
        } catch (e: Exception) {
          queue.logger.error("unable to send bookmark: ", e)
          if (this.retriesLeft == 0) {
            throw e
          } else {
            TimeUnit.SECONDS.sleep(queue.bookmarkSendRetrySleepSeconds)
          }
        }
      }
    }
  }

  /**
   * Delete a bookmark from the server.
   */

  data class CommandDeleteBookmark(
    private val accountID: AccountID,
    private val accountAuthenticationCredentials: AccountAuthenticationCredentials,
    private val bookmark: ReaderBookmark,
    private val retries: Int)
    : Command<Unit> {

    private var retriesLeft = this.retries

    override fun execute(
      future: SettableFuture<Unit>,
      queue: ReaderBookmarkServerCommandQueue) {
      while (true) {
        if (future.isCancelled) {
          throw CancellationException()
        }

        val uri = bookmark.uri
        if (uri == null) {
          queue.logger.debug("bookmark {} cannot be deleted; no URI", this.bookmark.bookmarkId.value)
          throw IllegalArgumentException("No URI for bookmark")
        }

        try {
          --this.retriesLeft
          queue.httpCalls.bookmarkDelete(uri, this.accountAuthenticationCredentials)
          queue.logger.debug("bookmark {} deleted successfully", this.bookmark.bookmarkId.value)
          return
        } catch (e: Exception) {
          queue.logger.error("unable to send bookmark: ", e)
          if (this.retriesLeft == 0) {
            throw e
          } else {
            TimeUnit.SECONDS.sleep(queue.bookmarkDeleteRetrySleepSeconds)
          }
        }
      }
    }
  }

  /**
   * Receive bookmarks from the server.
   */

  class CommandReceiveBookmarks(
    val accountID: AccountID,
    val targetURI: URI,
    val accountAuthenticationCredentials: AccountAuthenticationCredentials)
    : Command<List<ReceivedBookmark>> {

    override fun execute(
      future: SettableFuture<List<ReceivedBookmark>>,
      queue: ReaderBookmarkServerCommandQueue): List<ReceivedBookmark> {
      return queue.httpCalls.bookmarksGet(this.targetURI, this.accountAuthenticationCredentials)
        .map { annotation -> parseBookmarkOrNull(queue.logger, queue.objectMapper, annotation) }
        .filter { bookmark -> bookmark != null }
        .map { bookmark -> ReceivedBookmark(this.accountID, bookmark!!) }
    }

    private fun parseBookmarkOrNull(
      logger: Logger,
      objectMapper: ObjectMapper,
      annotation: BookmarkAnnotation): ReaderBookmark? {
      return try {
        BookmarkAnnotations.toBookmark(objectMapper, annotation)
      } catch (e: Exception) {
        logger.error("unable to parse bookmark: ", e)
        null
      }
    }
  }

  /**
   * Receive bookmarks from the server.
   */

  class CommandSyncingIsEnabled(
    val targetURI: URI,
    val accountAuthenticationCredentials: AccountAuthenticationCredentials)
    : Command<Boolean> {

    override fun execute(
      future: SettableFuture<Boolean>,
      queue: ReaderBookmarkServerCommandQueue): Boolean {
      return queue.httpCalls.syncingIsEnabled(
        this.targetURI, this.accountAuthenticationCredentials)
    }
  }

  /**
   * Receive bookmarks from the server.
   */

  class CommandSyncingEnable(
    val targetURI: URI,
    val accountAuthenticationCredentials: AccountAuthenticationCredentials,
    val enabled: Boolean)
    : Command<Unit> {

    override fun execute(
      future: SettableFuture<Unit>,
      queue: ReaderBookmarkServerCommandQueue) {
      return queue.httpCalls.syncingEnable(
        this.targetURI, this.accountAuthenticationCredentials, this.enabled)
    }
  }

  private fun <T> enqueueCommand(command: Command<T>): FluentFuture<T> {
    if (this.closed.get()) {
      throw IllegalStateException("Queue is closed")
    }

    val future = SettableFuture.create<T>()
    this.executor.execute {
      try {
        future.set(command.execute(future, this))
      } catch (e: Throwable) {
        future.setException(e)
      }
    }
    return FluentFuture.from(future)
  }

  override fun isRunning(): Boolean {
    return !this.closed.get()
  }

  override fun sendBookmark(
    accountID: AccountID,
    targetURI: URI,
    credentials: AccountAuthenticationCredentials,
    bookmark: ReaderBookmark): FluentFuture<Unit> {

    return this.enqueueCommand(
      CommandSendBookmark(accountID, targetURI, credentials, bookmark, this.bookmarkSendRetries))
  }

  override fun receiveBookmarks(
    accountID: AccountID,
    targetURI: URI,
    credentials: AccountAuthenticationCredentials): FluentFuture<List<ReceivedBookmark>> {
    return this.enqueueCommand(CommandReceiveBookmarks(accountID, targetURI, credentials))
  }

  override fun deleteBookmark(
    accountID: AccountID,
    credentials: AccountAuthenticationCredentials,
    bookmark: ReaderBookmark): FluentFuture<Unit> {
    return this.enqueueCommand(
      CommandDeleteBookmark(accountID, credentials, bookmark, this.bookmarkDeleteRetries))
  }

  override fun syncingIsEnabled(
    settingsURI: URI,
    credentials: AccountAuthenticationCredentials): FluentFuture<Boolean> {
    return this.enqueueCommand(CommandSyncingIsEnabled(settingsURI, credentials))
  }

  override fun syncingEnable(
    settingsURI: URI,
    credentials: AccountAuthenticationCredentials,
    enabled: Boolean): FluentFuture<Unit> {
    return this.enqueueCommand(CommandSyncingEnable(settingsURI, credentials, enabled))
  }
}
