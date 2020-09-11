package org.nypl.simplified.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
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
  val clock: () -> Instant
  val httpClient: LSHTTPClientType
  val taskRecorder: TaskRecorderType

  /**
   * A flag that indicates a borrow task has been cancelled. Subtasks should take care to
   * observe this flag during long-running operations in order to support cancellation.
   *
   * @return `true` if the borrow task has been cancelled
   */

  val isCancelled: Boolean

  /**
   * The current acquisition path element. This will be updated once for each subtask.
   */

  val currentURI: URI?

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

  fun bookIsDownloading(
    expectedSize: Long?,
    receivedSize: Long,
    bytesPerSecond: Long
  )
}
