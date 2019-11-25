package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.slf4j.LoggerFactory

import java.util.concurrent.Callable

internal class BookDeleteTask(
  private val account: AccountType,
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

    val entry = this.account.bookDatabase.entry(this.bookId)
    entry.delete()
    this.bookRegistry.clearFor(this.bookId)
  }
}
