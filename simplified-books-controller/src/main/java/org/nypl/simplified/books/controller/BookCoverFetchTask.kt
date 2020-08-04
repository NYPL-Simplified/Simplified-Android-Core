package org.nypl.simplified.books.controller

import android.net.Uri
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledURIs.BUNDLED_CONTENT_SCHEME
import org.nypl.simplified.books.controller.BookCoverFetchTask.Type.COVER
import org.nypl.simplified.books.controller.BookCoverFetchTask.Type.THUMBNAIL
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.Locale
import java.util.concurrent.Callable

/**
 * A task that fetches and saves the book cover if there is one.
 */

class BookCoverFetchTask(
  private val services: BookTaskRequiredServices,
  private val databaseEntry: BookDatabaseEntryType,
  private val feedEntry: OPDSAcquisitionFeedEntry,
  private val type: Type,
  private val httpAuth: OptionType<HTTPAuthType>
) : Callable<TaskResult<BookStatusDownloadErrorDetails, Unit>> {

  enum class Type {
    COVER, THUMBNAIL;

    override fun toString(): String {
      return super.toString().toLowerCase(Locale.US)
    }
  }

  private val logger =
    LoggerFactory.getLogger(BookCoverFetchTask::class.java)
  private val taskRecorder =
    TaskRecorder.create<BookStatusDownloadErrorDetails>()

  override fun call(): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    this.taskRecorder.beginNewStep(this.services.borrowStrings.borrowBookFetchingCover)

    return try {
      val coverOpt = when (this.type) {
        COVER -> feedEntry.cover
        THUMBNAIL -> feedEntry.thumbnail
      }
      this.logger.debug("fetching {}: {}", this.type, coverOpt)

      if (coverOpt is Some<URI>) {
        val cover = coverOpt.get()

        when (cover.scheme) {
          "content" ->
            this.fetchContentURI(cover)
          BUNDLED_CONTENT_SCHEME ->
            this.fetchBundledURI(cover)
          else ->
            this.fetchCoverHTTP(cover)
        }
      } else {
        this.taskRecorder.currentStepSucceeded(this.services.borrowStrings.borrowBookFetchingCover)
        this.taskRecorder.finishSuccess(Unit)
      }
    } catch (e: Throwable) {
      this.logger.error("failed to fetch {}: ", this.type, e)

      this.taskRecorder.currentStepFailedAppending(
        this.services.borrowStrings.borrowBookCoverUnexpectedException,
        BookStatusDownloadErrorDetails.UnexpectedException(e),
        e)
      this.taskRecorder.finishFailure()
    } finally {

      /*
       * Refresh the entry in the book registry so that anything that's observing the book
       * will see the new cover.
       */

      this.services.bookRegistry.book(this.databaseEntry.book.id).map { withStatus ->
        this.services.bookRegistry.update(BookWithStatus(this.databaseEntry.book, withStatus.status))
      }
    }
  }

  private fun fetchBundledURI(
    cover: URI
  ): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    return this.services.bundledContent.resolve(cover).use(this::saveCover)
  }

  private fun fetchContentURI(
    cover: URI
  ): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    val inputStream = this.services.contentResolver.openInputStream(Uri.parse(cover.toString()))
    if (inputStream == null) {
      val message = this.services.borrowStrings.borrowBookContentCopyFailed
      this.taskRecorder.currentStepFailed(
        message = message,
        errorValue = BookStatusDownloadErrorDetails.ContentCopyFailed(message, mapOf()),
        exception = FileNotFoundException(cover.toString()))
      return this.taskRecorder.finishFailure()
    }
    return inputStream.use(this::saveCover)
  }

  private fun fetchCoverHTTP(
    cover: URI
  ): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    return when (val result = this.services.http.get(this.httpAuth, cover, 0L)) {
      is HTTPResultOK -> {
        this.saveCoverHTTP(result)
      }
      is HTTPResultError -> {
        this.taskRecorder.currentStepFailed(
          this.services.borrowStrings.borrowBookCoverUnexpectedException,
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
          this.services.borrowStrings.borrowBookCoverUnexpectedException,
          BookStatusDownloadErrorDetails.UnexpectedException(result.error),
          result.error)
        this.taskRecorder.finishFailure()
      }
      else -> throw UnreachableCodeException()
    }
  }

  private fun saveCoverHTTP(
    result: HTTPResultOK<InputStream>
  ): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    return this.saveCover(result.value)
  }

  private fun saveCover(
    inputStream: InputStream
  ): TaskResult.Success<BookStatusDownloadErrorDetails, Unit> {
    this.taskRecorder.beginNewStep(this.services.borrowStrings.borrowBookSavingCover)
    val file = this.databaseEntry.temporaryFile()
    FileOutputStream(file).use { stream ->
      inputStream.copyTo(stream)

      when (this.type) {
        COVER -> this.databaseEntry.setCover(file)
        THUMBNAIL -> this.databaseEntry.setThumbnail(file)
      }
    }
    return this.taskRecorder.finishSuccess(Unit)
  }

  private fun <T> someOrNull(x: OptionType<T>): T? =
    if (x is Some<T>) {
      x.get()
    } else null
}
