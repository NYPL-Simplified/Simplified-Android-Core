package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Some
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A task that dismisses a download.
 */

class BookBorrowFailedDismissTask(
  accountID: AccountID,
  profileID: ProfileID,
  profiles: ProfilesDatabaseType,
  private val bookID: BookID,
  private val bookRegistry: BookRegistryType,
) : AbstractBookTask(accountID, profileID, profiles) {

  override val logger: Logger =
    LoggerFactory.getLogger(BookBorrowFailedDismissTask::class.java)

  override val taskRecorder: TaskRecorderType =
    TaskRecorder.create()

  @Throws(Exception::class)
  override fun execute(account: AccountType): TaskResult.Success<Unit> {
    this.logger.debug("acknowledging download of book {}", this.bookID)
    this.taskRecorder.beginNewStep("Starting borrow failed dismiss task...")

    val statusOpt = this.bookRegistry.bookStatus(this.bookID)
    if (statusOpt is Some<BookStatus>) {
      val status = statusOpt.get()
      this.logger.debug("status of book {} is currently {}", this.bookID, status)
      val entry = account.bookDatabase.entry(this.bookID)
      val book = entry.book
      this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
    }
    return this.taskRecorder.finishSuccess(Unit)
  }

  override fun onFailure(result: TaskResult.Failure<Unit>) {
    // Nothing to do
  }
}
