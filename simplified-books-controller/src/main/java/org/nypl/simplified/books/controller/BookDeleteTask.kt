package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class BookDeleteTask(
  private val accountId: AccountID,
  private val profiles: ProfilesDatabaseType,
  private val bookRegistry: BookRegistryType,
  private val bookId: BookID
) : Callable<Unit> {

  private val logger = LoggerFactory.getLogger(BookDeleteTask::class.java)

  @Throws(Exception::class)
  override fun call() {
    this.execute()
  }

  @Throws(BookDatabaseException::class)
  private fun execute() {
    this.logger.debug("[{}] deleting book", this.bookId.brief())

    try {
      val profile = this.profiles.currentProfileUnsafe()
      val account = profile.account(this.accountId)
      val entry = account.bookDatabase.entry(this.bookId)
      entry.delete()
    } finally {
      this.bookRegistry.clearFor(this.bookId)
    }
  }
}
