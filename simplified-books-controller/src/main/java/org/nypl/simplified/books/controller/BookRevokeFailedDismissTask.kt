package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit

import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.slf4j.LoggerFactory

import java.util.concurrent.Callable

class BookRevokeFailedDismissTask(
  private val bookDatabase: BookDatabaseType,
  private val bookRegistry: BookRegistryType,
  private val bookId: BookID
) : Callable<Unit> {

  private val logger =
    LoggerFactory.getLogger(BookRevokeFailedDismissTask::class.java)

  @Throws(Exception::class)
  override fun call(): Unit {
    try {
      this.logger.debug("[{}] revoke failure dismiss", this.bookId.brief())

      val statusOpt = this.bookRegistry.bookStatus(this.bookId)
      if (statusOpt is Some<BookStatus>) {
        val status = statusOpt.get()
        this.logger.debug("[{}] status of book is currently {}", this.bookId.brief(), status)
        val entry = this.bookDatabase.entry(this.bookId)
        val book = entry.book
        val newStatus = BookStatus.fromBook(book)
        this.bookRegistry.update(BookWithStatus(book, newStatus))
        this.logger.debug("[{}] status of book is now {}", this.bookId.brief(), newStatus)
      }
    } finally {
      this.logger.debug("[{}] revoke failure dismiss finished", this.bookId.brief())
    }
    return Unit.unit()
  }
}
