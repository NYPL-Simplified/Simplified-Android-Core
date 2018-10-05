package org.nypl.simplified.tests.books

import com.io7m.jfunctional.OptionType

import org.nypl.simplified.books.core.AccountSyncListenerType
import org.nypl.simplified.books.core.BookID
import org.slf4j.Logger

/**
 * An account sync listener that simply logs everything.
 */

open class LoggingSyncListener(private val logger: Logger) : AccountSyncListenerType {

  override fun onAccountSyncAuthenticationFailure(message: String) {
    this.logger.error("onAccountSyncAuthenticationFailure: {}", message)
  }

  override fun onAccountSyncBook(book: BookID) {
    this.logger.debug("onAccountSyncBook: {}", book)
  }

  override fun onAccountSyncFailure(error: OptionType<Throwable>, message: String) {
    this.logger.error("onAccountSyncFailure: {} {}", error, message)
  }

  override fun onAccountSyncSuccess() {
    this.logger.debug("onAccountSyncSuccess")
  }

  override fun onAccountSyncBookDeleted(book: BookID) {
    this.logger.debug("onAccountSyncBookDeleted: {}", book)
  }
}
