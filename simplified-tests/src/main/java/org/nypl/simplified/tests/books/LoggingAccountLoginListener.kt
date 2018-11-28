package org.nypl.simplified.tests.books

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountLoginListenerType
import org.nypl.simplified.books.core.BookID
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger

/**
 * An account login listener that can have its progress tracked by waiting on
 * the given latches.
 */

open class LoggingAccountLoginListener(private val logger: Logger) : AccountLoginListenerType {

  val syncedBookCount = AtomicInteger()

  override fun onAccountSyncAuthenticationFailure(message: String) {
    this.logger.debug("onAccountSyncAuthenticationFailure: {}", message)
  }

  override fun onAccountSyncBook(book: BookID) {
    this.logger.debug("onAccountSyncBook: {}", book)
    this.syncedBookCount.incrementAndGet()
  }

  override fun onAccountSyncFailure(error: OptionType<Throwable>, message: String) {
    this.logger.debug("onAccountSyncFailure: {} {}", error, message)
  }

  override fun onAccountSyncSuccess() {
    this.logger.debug("onAccountSyncSuccess")
  }

  override fun onAccountSyncBookDeleted(book: BookID) {
    this.logger.debug("onAccountSyncBookDeleted: {}", book)
  }

  override fun onAccountLoginFailureCredentialsIncorrect() {
    this.logger.debug("onAccountLoginFailureCredentialsIncorrect")
  }

  override fun onAccountLoginFailureServerError(code: Int) {
    this.logger.debug("onAccountLoginFailureServerError: {}", code)
  }

  override fun onAccountLoginFailureLocalError(error: OptionType<Throwable>, message: String) {
    this.logger.debug("onAccountLoginFailureLocalError: {} {}", error, message)
  }

  override fun onAccountLoginSuccess(credentials: AccountCredentials) {
    this.logger.debug("onAccountLoginSuccess: {}", credentials)
  }

  override fun onAccountLoginFailureDeviceActivationError(message: String) {
    this.logger.debug("onAccountLoginFailureDeviceActivationError: {}", message)
  }
}
