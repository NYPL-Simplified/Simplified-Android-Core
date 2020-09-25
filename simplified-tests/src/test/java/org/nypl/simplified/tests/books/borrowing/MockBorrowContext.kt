package org.nypl.simplified.tests.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.BorrowTimeoutConfiguration
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.tests.TestDirectories
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

class MockBorrowContext(
  val logger: Logger,
  val temporaryDirectory: File,
  val bookRegistry: BookRegistryType,
  override var bundledContent: BundledContentResolverType,
  override var account: AccountReadableType,
  override var clock: () -> Instant,
  override var contentResolver: ContentResolverType,
  override var httpClient: LSHTTPClientType,
  override var taskRecorder: TaskRecorderType,
  override var isCancelled: Boolean,
  override var bookDatabaseEntry: BookDatabaseEntryType,
  bookInitial: Book
) : BorrowContextType {

  var cacheDirectory = TestDirectories.temporaryDirectory()

  override fun cacheDirectory(): File {
    return this.cacheDirectory
  }

  override lateinit var audioBookManifestStrategies: AudioBookManifestStrategiesType
  override lateinit var services: ServiceDirectoryType

  override var adobeExecutorTimeout: BorrowTimeoutConfiguration =
    BorrowTimeoutConfiguration(2L, TimeUnit.SECONDS)

  override var adobeExecutor: AdobeAdeptExecutorType? = null
  override lateinit var currentAcquisitionPathElement: OPDSAcquisitionPathElement
  override lateinit var opdsAcquisitionPath: OPDSAcquisitionPath
  override var bookCurrent: Book = bookInitial

  override fun bookPublishStatus(status: BookStatus) {
    val bookNext = this.bookDatabaseEntry.book
    this.bookCurrent = bookNext
    this.bookRegistry.update(BookWithStatus(bookNext, status))
  }

  override fun bookDownloadSucceeded() {
    val book = this.bookDatabaseEntry.book
    check(book.isDownloaded)
    val status = BookStatus.fromBook(book)
    this.bookPublishStatus(status)
  }

  override fun bookDownloadIsRunning(
    expectedSize: Long?,
    receivedSize: Long,
    bytesPerSecond: Long,
    message: String
  ) {
    this.logDebug("downloading: {} {} {}", expectedSize, receivedSize, bytesPerSecond)

    this.bookPublishStatus(
      BookStatus.Downloading(
        id = this.bookCurrent.id,
        currentTotalBytes = receivedSize,
        expectedTotalBytes = expectedSize ?: 100L,
        detailMessage = message
      )
    )
  }

  override fun bookDownloadFailed() {
    this.bookPublishStatus(
      BookStatus.FailedDownload(
        id = this.bookCurrent.id,
        result = this.taskRecorder.finishFailure()
      )
    )
  }

  override fun bookLoanIsRequesting(message: String) {
    this.bookPublishStatus(
      BookStatus.RequestingLoan(
        id = this.bookCurrent.id,
        detailMessage = message
      )
    )
  }

  override fun bookLoanFailed() {
    this.bookPublishStatus(
      BookStatus.FailedLoan(
        id = this.bookCurrent.id,
        result = this.taskRecorder.finishFailure()
      )
    )
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
