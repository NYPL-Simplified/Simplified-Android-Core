package org.nypl.simplified.tests.books

import com.io7m.jfunctional.OptionType

import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountLoginListenerType
import org.nypl.simplified.books.core.BookID
import org.slf4j.Logger

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * An account login listener that can have its progress tracked by waiting on
 * the given latches.
 */

open class LatchedAccountLoginListener(
  private val logger: Logger,
  private val login_sync_latch: CountDownLatch,
  private val synced_book_count: AtomicInteger,
  private val synced_ok: AtomicBoolean,
  private val login_latch: CountDownLatch) : AccountLoginListenerType {

  override fun onAccountSyncAuthenticationFailure(message: String) {
    this.logger.debug("onAccountSyncAuthenticationFailure: {}", message)
    this.login_sync_latch.countDown()
  }

  override fun onAccountSyncBook(book: BookID) {
    this.logger.debug("onAccountSyncBook: {}", book)
    this.synced_book_count.incrementAndGet()
  }

  override fun onAccountSyncFailure(
    error: OptionType<Throwable>,
    message: String) {
    this.logger.debug("onAccountSyncFailure: {} {}", error, message)
    this.login_sync_latch.countDown()
  }

  override fun onAccountSyncSuccess() {
    this.logger.debug("onAccountSyncSuccess")
    this.synced_ok.set(true)
    this.login_sync_latch.countDown()
  }

  override fun onAccountSyncBookDeleted(book: BookID) {
    this.logger.debug("onAccountSyncBookDeleted: {}", book)
  }

  override fun onAccountLoginFailureCredentialsIncorrect() {
    try {
      this.logger.debug("onAccountLoginFailureCredentialsIncorrect")
    } finally {
      this.login_latch.countDown()
    }
  }

  override fun onAccountLoginFailureServerError(code: Int) {
    try {
      this.logger.debug("onAccountLoginFailureServerError: {}", code)
    } finally {
      this.login_latch.countDown()
    }
  }

  override fun onAccountLoginFailureLocalError(
    error: OptionType<Throwable>,
    message: String) {
    this.logger.debug("onAccountLoginFailureLocalError: {} {}", error, message)
  }

  override fun onAccountLoginSuccess(
    credentials: AccountCredentials) {
    try {
      this.logger.debug("onAccountLoginSuccess: {}", credentials)
    } finally {
      this.login_latch.countDown()
    }
  }

  override fun onAccountLoginFailureDeviceActivationError(message: String) {
    this.logger.debug("onAccountLoginFailureDeviceActivationError: {}", message)
  }
}
