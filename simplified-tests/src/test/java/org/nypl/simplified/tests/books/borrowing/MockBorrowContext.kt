package org.nypl.simplified.tests.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID

class MockBorrowContext(
  val logger: Logger,
  val temporaryDirectory: File,
  override var account: AccountReadableType,
  override var clock: () -> Instant,
  override var httpClient: LSHTTPClientType,
  override var taskRecorder: TaskRecorderType,
  override var isCancelled: Boolean,
  override var bookDatabaseEntry: BookDatabaseEntryType,
  bookInitial: Book
) : BorrowContextType {

  override lateinit var currentAcquisitionPathElement: OPDSAcquisitionPathElement
  override lateinit var opdsAcquisitionPath: OPDSAcquisitionPath
  override var bookCurrent: Book = bookInitial

  override fun bookIsDownloading(
    expectedSize: Long?,
    receivedSize: Long,
    bytesPerSecond: Long
  ) {
    this.logDebug("downloading: {} {} {}", expectedSize, receivedSize, bytesPerSecond)
  }

  var currentRemainingOPDSPathElements: List<OPDSAcquisitionPathElement> =
    listOf()

  var currentURIField: URI? =
    null

  override fun currentURI(): URI? {
    return this.currentURIField ?: return this.currentAcquisitionPathElement.target
  }

  private val receivedURIsData =
    mutableListOf<URI>()

  val receivedURIs: List<URI>
    get() = this.receivedURIsData

  override fun receivedNewURI(uri: URI) {
    this.logDebug("received new URI: {}", uri)
    this.receivedURIsData.add(uri)
    this.currentURIField = uri
  }

  private val bookIdBrief =
    this.bookCurrent.id.brief()

  override fun logDebug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}] $message", this.bookIdBrief, *arguments)

  override fun logError(message: String, vararg arguments: Any?) =
    this.logger.error("[{}] $message", this.bookIdBrief, *arguments)

  override fun logWarn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}] $message", this.bookIdBrief, *arguments)

  override fun temporaryFile(): File {
    this.temporaryDirectory.mkdirs()
    for (i in 0..100) {
      val file = File(this.temporaryDirectory, "${UUID.randomUUID()}.tmp")
      if (!file.exists()) {
        return file
      }
    }
    throw IOException("Could not create a temporary file within 100 attempts!")
  }

  override fun opdsAcquisitionPathRemaining(): List<OPDSAcquisitionPathElement> {
    return this.currentRemainingOPDSPathElements
  }
}
