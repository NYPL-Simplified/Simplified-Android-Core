package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BookDeleteTask(
  accountID: AccountID,
  profileID: ProfileID,
  profiles: ProfilesDatabaseType,
  private val bookID: BookID,
  private val bookRegistry: BookRegistryType,
) : AbstractBookTask(accountID, profileID, profiles) {

  override val logger: Logger =
    LoggerFactory.getLogger(BookDeleteTask::class.java)

  override val taskRecorder: TaskRecorderType =
    TaskRecorder.create()

  @Throws(BookDatabaseException::class)
  override fun execute(account: AccountType): TaskResult.Success<Unit> {
    this.logger.debug("[{}] deleting book", this.bookID.brief())
    this.taskRecorder.beginNewStep("Deleting book...")

    return try {
      val entry = account.bookDatabase.entry(this.bookID)
      entry.delete()
      this.taskRecorder.finishSuccess(Unit)
    } catch (e: Exception) {
      this.taskRecorder.currentStepFailed(
        message = e.message ?: e.javaClass.canonicalName ?: "unknown",
        errorCode = "deleteFailed",
        exception = e
      )
      throw TaskFailedHandled(e)
    } finally {
      this.bookRegistry.clearFor(this.bookID)
    }
  }

  override fun onFailure(result: TaskResult.Failure<Unit>) {
    // Nothing to do
  }
}
