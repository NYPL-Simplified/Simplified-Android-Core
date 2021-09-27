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

class BookRevokeFailedDismissTask(
  accountID: AccountID,
  profileID: ProfileID,
  profiles: ProfilesDatabaseType,
  private val bookID: BookID,
  private val bookRegistry: BookRegistryType,
) : AbstractBookTask(accountID, profileID, profiles) {

  override val logger: Logger =
    LoggerFactory.getLogger(BookRevokeFailedDismissTask::class.java)

  override val taskRecorder: TaskRecorderType =
    TaskRecorder.create()

  @Throws(Exception::class)
  override fun execute(account: AccountType): TaskResult.Success<Unit> {
    this.logger.debug("[{}] revoke failure dismiss", this.bookID.brief())
    this.taskRecorder.beginNewStep("Dismissing failed revocation...")

    return try {
      val statusOpt = this.bookRegistry.bookStatus(this.bookID)
      if (statusOpt is Some<BookStatus>) {
        val status = statusOpt.get()
        this.logger.debug("[{}] status of book is currently {}", this.bookID.brief(), status)
        val entry = account.bookDatabase.entry(this.bookID)
        val book = entry.book
        val newStatus = BookStatus.fromBook(book)
        this.bookRegistry.update(BookWithStatus(book, newStatus))
        this.logger.debug("[{}] status of book is now {}", this.bookID.brief(), newStatus)
      }
      this.taskRecorder.finishSuccess(Unit)
    } finally {
      this.logger.debug("[{}] revoke failure dismiss finished", this.bookID.brief())
    }
  }

  override fun onFailure(result: TaskResult.Failure<Unit>) {
    // Nothing to do
  }
}
