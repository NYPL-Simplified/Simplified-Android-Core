package org.nypl.simplified.books.controller

import android.content.ContentResolver
import org.joda.time.Duration
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookAcquisitionSelection
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * A task that borrows a book using the default acquisition.
 */

class BookBorrowWithDefaultAcquisitionTask(
  private val accountId: AccountID,
  private val bookId: BookID,
  private val borrowTimeoutDuration: Duration = Duration.standardMinutes(1L),
  private val cacheDirectory: File,
  private val contentResolver: ContentResolver,
  private val downloads: ConcurrentHashMap<BookID, DownloadType>,
  private val downloadTimeoutDuration: Duration = Duration.standardMinutes(3L),
  private val entry: OPDSAcquisitionFeedEntry,
  private val profiles: ProfilesDatabaseType,
  private val services: ServiceDirectoryType
) : Callable<TaskResult<BookStatusDownloadErrorDetails, Unit>> {

  private val logger =
    LoggerFactory.getLogger(BookBorrowWithDefaultAcquisitionTask::class.java)
  private val borrowStrings =
    this.services.requireService(BookBorrowStringResourcesType::class.java)
  private val bookRegistry =
    this.services.requireService(BookRegistryType::class.java)

  /**
   * The initial book value.
   */

  private val bookInitial =
    Book(
      id = this.bookId,
      account = this.accountId,
      cover = null,
      thumbnail = null,
      entry = this.entry,
      formats = listOf()
    )

  override fun call(): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    val acquisition =
      BookAcquisitionSelection.preferredAcquisition(this.entry.acquisitions)

    if (acquisition == null) {
      this.logger.error("[{}]: no usable acquisition!", this.bookId.brief())

      val failure =
        TaskResult.fail<BookStatusDownloadErrorDetails, Unit>(
          description = this.borrowStrings.borrowBookSelectingAcquisition,
          resolution = this.borrowStrings.borrowBookFulfillNoUsableAcquisitions,
          errorValue = BookStatusDownloadErrorDetails.UnusableAcquisitions(
            message = this.borrowStrings.borrowBookFulfillNoUsableAcquisitions,
            attributes = mapOf()
          )
        ) as TaskResult.Failure<BookStatusDownloadErrorDetails, Unit>

      this.bookRegistry.update(
        BookWithStatus(this.bookInitial, BookStatus.FailedLoan(this.bookId, failure)))

      return failure
    }

    return BookBorrowTask(
      accountId = accountId,
      acquisition = acquisition,
      bookId = this.bookId,
      borrowTimeoutDuration = this.borrowTimeoutDuration,
      cacheDirectory = this.cacheDirectory,
      contentResolver = this.contentResolver,
      downloads = this.downloads,
      downloadTimeoutDuration = this.downloadTimeoutDuration,
      entry = this.entry,
      profiles = this.profiles,
      services = this.services
    ).call()
  }
}
