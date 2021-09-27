package org.nypl.simplified.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AxisNowServiceType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.accountsDatabaseException
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.bookDatabaseFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.noSubtaskAvailable
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.noSupportedAcquisitions
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.profileNotFound
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.subtaskFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.unexpectedException
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskCancelled
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskHaltedEarly
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepFailed
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepSucceeded
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The primary borrow task implementation.
 */

class BorrowTask private constructor(
  private val requirements: BorrowRequirements,
  private val request: BorrowRequest
) : BorrowTaskType {

  companion object : BorrowTaskFactoryType {
    override fun createBorrowTask(
      requirements: BorrowRequirements,
      request: BorrowRequest
    ): BorrowTaskType {
      return BorrowTask(
        requirements = requirements,
        request = request
      )
    }
  }

  private val logger =
    LoggerFactory.getLogger(BorrowTask::class.java)

  private val cancelled =
    AtomicBoolean(false)

  private val bookId by lazy {
    BookIDs.newFromOPDSEntry(this.request.opdsAcquisitionFeedEntry)
  }

  private val bookIdBrief by lazy {
    bookId.brief()
  }

  private var databaseEntry: BookDatabaseEntryType? = null
  private lateinit var account: AccountType
  private lateinit var taskRecorder: TaskRecorderType

  private class BorrowFailedHandled(exception: Throwable?) : Exception(exception)

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}] $message", this.bookIdBrief, *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}] $message", this.bookIdBrief, *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}] $message", this.bookIdBrief, *arguments)

  override fun execute(): TaskResult<*> {
    this.taskRecorder = TaskRecorder.create()
    this.debug("starting")

    return try {
      return when (val start = this.request) {
        is BorrowRequest.Start -> this.executeStart(start)
      }
    } catch (e: BorrowFailedHandled) {
      this.warn("handled: ", e)
      this.taskRecorder.finishFailure<Unit>()
    } catch (e: Throwable) {
      this.error("unhandled exception during borrowing: ", e)
      this.taskRecorder.currentStepFailedAppending(this.messageOrName(e), unexpectedException, e)
      this.taskRecorder.finishFailure<Unit>()
    }
  }

  private fun publishRequestingDownload(bookID: BookID) {
    this.requirements.bookRegistry.bookOrNull(bookID)?.let { bookWithStatus ->
      this.requirements.bookRegistry.update(
        BookWithStatus(
          book = bookWithStatus.book,
          status = BookStatus.RequestingDownload(bookID)
        )
      )
    }
  }

  override fun cancel() {
    this.cancelled.set(true)
  }

  private fun messageOrName(e: Throwable) =
    e.message ?: e.javaClass.name

  private fun executeStart(start: BorrowRequest.Start): TaskResult<*> {
    this.taskRecorder.addAttribute("Book", start.opdsAcquisitionFeedEntry.title)
    this.taskRecorder.addAttribute("Author", start.opdsAcquisitionFeedEntry.authorsCommaSeparated)
    this.taskRecorder.addAttribute("Profile ID", start.profileId.toString())

    this.publishRequestingDownload(this.bookId)

    /*
     * The initial book value. Note that this is a synthesized value because we need to be
     * able to open the book database to get a real book value, and that database call might
     * fail. If the call fails, we have no "book" that we can refer to in order to publish a
     * "book download has failed" status for the book, so we use this fake book in that (rare)
     * situation.
     */

    val bookInitial =
      Book(
        id = bookId,
        account = start.accountId,
        cover = null,
        thumbnail = null,
        entry = start.opdsAcquisitionFeedEntry,
        formats = listOf()
      )

    val profile = this.findProfile(start.profileId, bookInitial)
    this.account = this.findAccount(profile, bookInitial)
    val book = this.createBookDatabaseEntry(bookInitial, start.opdsAcquisitionFeedEntry)
    val path = this.pickAcquisitionPath(book, start.opdsAcquisitionFeedEntry)
    this.executeSubtasksForPath(book, path)
    return this.taskRecorder.finishSuccess(Unit)
  }

  /**
   * Execute all subtasks for the given acquisition path.
   */

  private fun executeSubtasksForPath(
    book: Book,
    path: OPDSAcquisitionPath
  ) {
    val context =
      BorrowContext(
        account = this.account,
        adobeExecutor = this.requirements.adobeExecutor,
        axisNowService = this.requirements.axisNowService,
        audioBookManifestStrategies = this.requirements.audioBookManifestStrategies,
        bookDatabaseEntry = this.databaseEntry!!,
        bookInitial = book,
        bookRegistry = this.requirements.bookRegistry,
        bundledContent = this.requirements.bundledContent,
        cacheDirectory = this.requirements.cacheDirectory,
        cancelled = this.cancelled,
        clock = this.requirements.clock,
        contentResolver = this.requirements.contentResolver,
        currentOPDSAcquisitionPathElement = path.elements.first(),
        httpClient = this.requirements.httpClient,
        logger = this.logger,
        opdsAcquisitionPath = path,
        services = this.requirements.services,
        taskRecorder = this.taskRecorder,
        temporaryDirectory = this.requirements.temporaryDirectory
      )

    val elementQueue = path.elements.toMutableList()
    while (elementQueue.isNotEmpty()) {
      try {
        val pathElement = elementQueue[0]
        elementQueue.removeAt(0)
        context.currentOPDSAcquisitionPathElement = pathElement
        context.currentRemainingOPDSPathElements = elementQueue.toList()
        val subtaskFactory = this.subtaskFindForPathElement(context, pathElement, book)
        this.subtaskExecute(subtaskFactory, context, book)
      } catch (e: BorrowSubtaskHaltedEarly) {
        this.logger.debug("subtask halted early: ", e)
        return
      } catch (e: BorrowSubtaskCancelled) {
        this.logger.debug("subtask cancelled: ", e)
        return
      }
    }
  }

  /**
   * Create and execute the given subtask.
   */

  private fun subtaskExecute(
    subtaskFactory: BorrowSubtaskFactoryType,
    context: BorrowContext,
    book: Book
  ) {
    val name = subtaskFactory.name
    val step = this.taskRecorder.beginNewStep("Executing subtask '$name'...")
    try {
      subtaskFactory.createSubtask().execute(context)
      step.resolution = TaskStepSucceeded("Executed subtask '$name' successfully.")
    } catch (e: BorrowSubtaskHaltedEarly) {
      throw e
    } catch (e: BorrowSubtaskCancelled) {
      throw e
    } catch (e: Exception) {
      step.resolution = TaskStepFailed(
        message = "Subtask '$name' raised an unexpected exception",
        exception = e,
        errorCode = subtaskFailed
      )
      this.publishBookFailure(book)
      throw BorrowFailedHandled(e)
    }
  }

  /**
   * Find a suitable subtask for the acquisition element.
   */

  private fun subtaskFindForPathElement(
    context: BorrowContext,
    pathElement: OPDSAcquisitionPathElement,
    book: Book
  ): BorrowSubtaskFactoryType {
    this.taskRecorder.beginNewStep("Finding subtask for acquisition path element...")
    val subtaskFactory =
      this.requirements.subtasks.findSubtaskFor(
        pathElement.mimeType,
        context.currentURI(),
        context.account
      )
    if (subtaskFactory == null) {
      this.taskRecorder.currentStepFailed(
        message = "We don't know how to handle this kind of acquisition.",
        errorCode = noSubtaskAvailable
      )
      this.publishBookFailure(book)
      throw BorrowFailedHandled(null)
    }
    val name = subtaskFactory.name
    this.taskRecorder.currentStepSucceeded("Found subtask '$name'")
    return subtaskFactory
  }

  /**
   * Create a new book database entry and publish the status of the book.
   */

  private fun createBookDatabaseEntry(
    book: Book,
    entry: OPDSAcquisitionFeedEntry
  ): Book {
    this.taskRecorder.beginNewStep("Setting up a book database entry...")

    try {
      val database = this.account.bookDatabase
      val dbEntry = database.createOrUpdate(book.id, entry)
      this.databaseEntry = dbEntry
      this.taskRecorder.currentStepSucceeded("Book database updated.")
      return dbEntry.book
    } catch (e: Exception) {
      this.error("[{}]: failed to set up book database: ", book.id.brief(), e)
      this.taskRecorder.currentStepFailed(
        message = "Could not set up the book database entry.",
        errorCode = bookDatabaseFailed,
        exception = e
      )
      this.publishBookFailure(book)
      throw BorrowFailedHandled(e)
    }
  }

  /**
   * Locate the given profile.
   */

  private fun findProfile(
    profileID: ProfileID,
    book: Book
  ): ProfileReadableType {
    this.taskRecorder.beginNewStep("Locating profile $profileID...")

    val profile = this.requirements.profiles.profiles()[profileID]
    return if (profile == null) {
      this.error("[{}]: failed to find profile: ", profileID)
      this.taskRecorder.currentStepFailed(
        message = "Failed to find profile.",
        errorCode = profileNotFound,
        exception = IllegalArgumentException()
      )
      this.publishBookFailure(book)
      throw BorrowFailedHandled(null)
    } else {
      this.taskRecorder.currentStepSucceeded("Located profile.")
      profile
    }
  }

  /**
   * Locate the account in the current profile.
   */

  private fun findAccount(
    profile: ProfileReadableType,
    book: Book
  ): AccountType {
    this.taskRecorder.beginNewStep("Locating account ${book.account.uuid} in the profile...")
    this.taskRecorder.addAttribute("Account ID", book.account.uuid.toString())

    val account = try {
      profile.account(this.request.accountId)
    } catch (e: Throwable) {
      this.error("[{}]: failed to find account: ", book.id.brief(), e)
      this.taskRecorder.currentStepFailedAppending(
        message = "An unexpected exception was raised.",
        errorCode = accountsDatabaseException,
        exception = e
      )

      this.publishBookFailure(book)
      throw BorrowFailedHandled(e)
    }

    this.taskRecorder.addAttribute("Account", account.provider.displayName)
    this.taskRecorder.currentStepSucceeded("Located account.")
    return account
  }

  /**
   * Pick the best available acquisition path.
   */

  private fun pickAcquisitionPath(
    book: Book,
    entry: OPDSAcquisitionFeedEntry
  ): OPDSAcquisitionPath {
    this.taskRecorder.beginNewStep("Planning the borrow operation…")

    val path = BorrowAcquisitions.pickBestAcquisitionPath(this.requirements.bookFormatSupport, entry)
    if (path == null) {
      this.taskRecorder.currentStepFailed("No supported acquisitions.", noSupportedAcquisitions)
      this.publishBookFailure(book)
      throw BorrowFailedHandled(null)
    }

    this.taskRecorder.currentStepSucceeded("Selected an acquisition path.")
    return path
  }

  private fun publishBookFailure(book: Book) {
    val failure = this.taskRecorder.finishFailure<Unit>()
    this.requirements.bookRegistry.update(BookWithStatus(book, BookStatus.FailedLoan(book.id, failure)))
  }

  private class BorrowContext(
    override val account: AccountReadableType,
    override val audioBookManifestStrategies: AudioBookManifestStrategiesType,
    override val clock: () -> Instant,
    override val contentResolver: ContentResolverType,
    override val bundledContent: BundledContentResolverType,
    override val bookDatabaseEntry: BookDatabaseEntryType,
    override val httpClient: LSHTTPClientType,
    override val taskRecorder: TaskRecorderType,
    override val opdsAcquisitionPath: OPDSAcquisitionPath,
    bookInitial: Book,
    private val bookRegistry: BookRegistryType,
    private val logger: Logger,
    private val temporaryDirectory: File,
    var currentOPDSAcquisitionPathElement: OPDSAcquisitionPathElement,
    override val adobeExecutor: AdobeAdeptExecutorType?,
    override val axisNowService: AxisNowServiceType?,
    override val services: ServiceDirectoryType,
    private val cacheDirectory: File,
    private val cancelled: AtomicBoolean
  ) : BorrowContextType {

    override fun cacheDirectory(): File =
      this.cacheDirectory

    override val adobeExecutorTimeout: BorrowTimeoutConfiguration =
      BorrowTimeoutConfiguration(300L, TimeUnit.SECONDS)

    override var isCancelled
      get() = this.cancelled.get()
      set(value) = this.cancelled.set(value)

    var currentRemainingOPDSPathElements =
      this.opdsAcquisitionPath.elements

    override val bookCurrent: Book
      get() = this.bookDatabaseEntry.book

    override fun bookDownloadIsWaitingForExternalAuthentication() {
      this.bookPublishStatus(
        BookStatus.DownloadWaitingForExternalAuthentication(
          id = this.bookCurrent.id,
          downloadURI = this.currentURICheck()
        )
      )
    }

    override fun bookDownloadIsRunning(
      message: String,
      receivedSize: Long?,
      expectedSize: Long?,
      bytesPerSecond: Long?
    ) {
      this.logDebug("downloading: {} {} {}", expectedSize, receivedSize, bytesPerSecond)

      this.bookPublishStatus(
        BookStatus.Downloading(
          id = this.bookCurrent.id,
          currentTotalBytes = receivedSize,
          expectedTotalBytes = expectedSize,
          detailMessage = message
        )
      )
    }

    override fun bookPublishStatus(status: BookStatus) {
      this.bookRegistry.update(BookWithStatus(this.bookDatabaseEntry.book, status))
    }

    override fun bookDownloadSucceeded() {
      this.bookPublishStatus(BookStatus.fromBook(this.bookDatabaseEntry.book))
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

    override fun bookDownloadFailed() {
      this.bookPublishStatus(
        BookStatus.FailedDownload(
          id = this.bookCurrent.id,
          result = this.taskRecorder.finishFailure()
        )
      )
    }

    var currentURIField: URI? =
      null

    override fun currentURI(): URI? {
      return this.currentURIField ?: return this.currentAcquisitionPathElement.target
    }

    override fun receivedNewURI(uri: URI) {
      this.logDebug("received new URI: {}", uri)
      this.currentURIField = uri
    }

    override val currentAcquisitionPathElement: OPDSAcquisitionPathElement
      get() = this.currentOPDSAcquisitionPathElement

    private val bookIdBrief =
      bookInitial.id.brief()

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
}
