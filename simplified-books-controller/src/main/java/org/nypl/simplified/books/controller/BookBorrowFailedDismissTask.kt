package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Some
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

/**
 * A task that dismisses a download.
 */

class BookBorrowFailedDismissTask(
  private val bookRegistry: BookRegistryType,
  private val id: BookID,
  private val bookDatabase: BookDatabaseType
) : Callable<Unit> {

  private val logger =
    LoggerFactory.getLogger(BookBorrowFailedDismissTask::class.java)

  override fun call() {
    this.logger.debug("acknowledging download of book {}", this.id)

    val statusOpt = this.bookRegistry.bookStatus(this.id)
    if (statusOpt is Some<BookStatus>) {
      val status = statusOpt.get()
      this.logger.debug("status of book {} is currently {}", this.id, status)
      val entry = this.bookDatabase.entry(this.id)
      val book = entry.book
      this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
    }
  }
}
