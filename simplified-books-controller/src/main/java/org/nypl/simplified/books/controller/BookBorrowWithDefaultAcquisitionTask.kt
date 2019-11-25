package org.nypl.simplified.books.controller

import org.joda.time.Duration
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookAcquisitionSelection
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * A task that borrows a book using the default acquisition.
 */

class BookBorrowWithDefaultAcquisitionTask(
  private val account: AccountType,
  private val bookId: BookID,
  private val borrowTimeoutDuration: Duration = Duration.standardMinutes(1L),
  private val cacheDirectory: File,
  private val downloads: ConcurrentHashMap<BookID, DownloadType>,
  private val downloadTimeoutDuration: Duration = Duration.standardMinutes(3L),
  private val entry: OPDSAcquisitionFeedEntry,
  private val services: ServiceDirectoryType
) : Callable<TaskResult<BookStatusDownloadErrorDetails, Unit>> {

  private val borrowStrings =
    this.services.requireService(BookBorrowStringResourcesType::class.java)

  override fun call(): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    val acquisition =
      BookAcquisitionSelection.preferredAcquisition(entry.acquisitions)
        ?: return TaskResult.fail(
          description = this.borrowStrings.borrowBookSelectingAcquisition,
          resolution = this.borrowStrings.borrowBookFulfillNoUsableAcquisitions,
          errorValue = BookStatusDownloadErrorDetails.UnusableAcquisitions(
            message = this.borrowStrings.borrowBookFulfillNoUsableAcquisitions,
            attributes = mapOf()
          )
        )

    return BookBorrowTask(
      account = account,
      acquisition = acquisition,
      bookId = bookId,
      borrowTimeoutDuration = borrowTimeoutDuration,
      cacheDirectory = this.cacheDirectory,
      downloads = this.downloads,
      downloadTimeoutDuration = downloadTimeoutDuration,
      entry = entry,
      services = this.services
    ).call()
  }

}