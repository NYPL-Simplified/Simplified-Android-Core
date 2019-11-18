package org.nypl.simplified.books.controller

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.concurrent.Callable

/**
 * A task that fetches and saves the book cover if there is one.
 */

class BookCoverFetchTask(
  private val bookRegistry: BookRegistryType,
  private val borrowStrings: BookBorrowStringResourcesType,
  private val databaseEntry: BookDatabaseEntryType,
  private val feedEntry: OPDSAcquisitionFeedEntry,
  private val http: HTTPType,
  private val httpAuth: OptionType<HTTPAuthType>
): Callable<TaskResult<BookStatusDownloadErrorDetails, Unit>> {

  private val taskRecorder = TaskRecorder.create<BookStatusDownloadErrorDetails>()

  override fun call(): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    this.taskRecorder.beginNewStep(this.borrowStrings.borrowBookFetchingCover)

    return try {
      val coverOpt = this.feedEntry.cover
      if (coverOpt is Some<URI>) {
        val cover = coverOpt.get()
        when (val result = this.http.get(this.httpAuth, cover, 0L)) {
          is HTTPResultOK -> {
            saveCover(result)
          }
          is HTTPResultError -> {
            this.taskRecorder.currentStepFailed(
              this.borrowStrings.borrowBookCoverUnexpectedException,
              BookStatusDownloadErrorDetails.HTTPRequestFailed(
                status = result.status,
                problemReport = this.someOrNull(result.problemReport),
                message = result.message,
                attributesInitial = mapOf()
              ))
            this.taskRecorder.finishFailure()
          }
          is HTTPResultException -> {
            this.taskRecorder.currentStepFailed(
              this.borrowStrings.borrowBookCoverUnexpectedException,
              BookStatusDownloadErrorDetails.UnexpectedException(result.error),
              result.error)
            this.taskRecorder.finishFailure()
          }
          else -> throw UnreachableCodeException()
        }
      } else {
        this.taskRecorder.currentStepSucceeded(this.borrowStrings.borrowBookFetchingCover)
        this.taskRecorder.finishSuccess(Unit)
      }
    } catch (e: Throwable) {
      this.taskRecorder.currentStepFailedAppending(
        this.borrowStrings.borrowBookCoverUnexpectedException,
        BookStatusDownloadErrorDetails.UnexpectedException(e),
        e)
      this.taskRecorder.finishFailure()
    } finally {

      /*
       * Refresh the entry in the book registry so that anything that's observing the book
       * will see the new cover.
       */

      this.bookRegistry.book(this.databaseEntry.book.id).map { withStatus ->
        this.bookRegistry.update(BookWithStatus(this.databaseEntry.book, withStatus.status))
      }
    }
  }

  private fun saveCover(result: HTTPResultOK<InputStream>): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    this.taskRecorder.beginNewStep(this.borrowStrings.borrowBookSavingCover)

    val file = this.databaseEntry.temporaryFile()
    FileOutputStream(file).use { stream ->
      result.value.copyTo(stream)
      this.databaseEntry.setCover(file)
    }
    return this.taskRecorder.finishSuccess(Unit)
  }

  private fun <T> someOrNull(x: OptionType<T>): T? =
    if (x is Some<T>) { x.get() } else null

}