package org.nypl.simplified.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskCancelled
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import java.io.File
import java.io.IOException
import java.net.URI

/**
 * The execution context of a borrowing operation.
 */

interface BorrowContextType {
  val account: AccountReadableType
  val adobeExecutor: AdobeAdeptExecutorType?
  val audioBookManifestStrategies: AudioBookManifestStrategiesType
  val bundledContent: BundledContentResolverType
  val clock: () -> Instant
  val contentResolver: ContentResolverType
  val httpClient: LSHTTPClientType
  val services: ServiceDirectoryType
  val taskRecorder: TaskRecorderType

  /**
   * The current cache directory.
   */

  fun cacheDirectory(): File

  /**
   * The timeout value that will be used when waiting for ACS operations to complete.
   */

  val adobeExecutorTimeout: BorrowTimeoutConfiguration

  /**
   * A flag that indicates a borrow task has been cancelled. Subtasks should take care to
   * observe this flag during long-running operations in order to support cancellation.
   *
   * @return `true` if the borrow task has been cancelled
   */

  val isCancelled: Boolean

  /**
   * Check to see if [isCancelled] is `true` and, if it is, throw [BorrowSubtaskCancelled].
   */

  @Throws(BorrowSubtaskCancelled::class)
  fun checkCancelled() {
    if (this.isCancelled) {
      this.taskRecorder.currentStepSucceeded("Task was cancelled.")
      throw BorrowSubtaskCancelled()
    }
  }

  /**
   * The current acquisition path element. This will be updated once for each subtask.
   */

  fun currentURI(): URI?

  /**
   * Check that the current URI is non-null. If the current URI is null, log an error
   * to the task recorder and throw [BorrowSubtaskFailed].
   */

  @Throws(BorrowSubtaskFailed::class)
  fun currentURICheck(): URI {
    val uri = this.currentURI()
    if (uri == null) {
      this.logError("no current URI")
      this.taskRecorder.currentStepFailed("A required URI is missing.", BorrowErrorCodes.requiredURIMissing)
      throw BorrowSubtaskFailed()
    }
    return uri
  }

  /**
   * The current subtask has received a new URI that can be used by the next subtask.
   */

  fun receivedNewURI(uri: URI)

  /**
   * The current acquisition path element. This will be updated once for each subtask.
   */

  val currentAcquisitionPathElement: OPDSAcquisitionPathElement

  /**
   * Convenience method to log at debug level.
   */

  fun logDebug(
    message: String,
    vararg arguments: Any?
  )

  /**
   * Convenience method to log at error level.
   */

  fun logError(
    message: String,
    vararg arguments: Any?
  )

  /**
   * Convenience method to log at warning level.
   */

  fun logWarn(
    message: String,
    vararg arguments: Any?
  )

  /**
   * Create a new temporary file.
   */

  @Throws(IOException::class)
  fun temporaryFile(): File

  /**
   * The full OPDS acquisition path that we are currently traversing.
   */

  val opdsAcquisitionPath: OPDSAcquisitionPath

  /**
   * The possibly-empty list of acquisition path elements that will be processed after the
   * current subtask completes.
   */

  fun opdsAcquisitionPathRemaining(): List<OPDSAcquisitionPathElement>

  /**
   * The current book database entry.
   */

  val bookDatabaseEntry: BookDatabaseEntryType

  /**
   * The current book state.
   */

  val bookCurrent: Book

  /**
   * Called by subtasks to indicate that a book is currently in the process of being downloaded.
   */

  fun bookDownloadIsRunning(
    expectedSize: Long?,
    receivedSize: Long,
    bytesPerSecond: Long,
    message: String
  )

  /**
   * Indicate that downloading the current book failed. Implementations should base the
   * actual resulting book status on the current status of the loan in the book database.
   */

  fun bookDownloadFailed()

  /**
   * Publish the latest status of the current book.
   */

  fun bookPublishStatus(status: BookStatus)

  /**
   * Indicate that downloading the current book succeeded. Implementations should base the
   * actual resulting book status on the current status of the loan in the book database.
   */

  fun bookDownloadSucceeded()

  /**
   * Indicate a request is being made to loan the current book. Implementations should base the
   * actual resulting book status on the current status of the loan in the book database.
   */

  fun bookLoanIsRequesting(message: String)

  /**
   * Indicate that borrowing the current book failed. Implementations should base the
   * actual resulting book status on the current status of the loan in the book database.
   */

  fun bookLoanFailed()
}
