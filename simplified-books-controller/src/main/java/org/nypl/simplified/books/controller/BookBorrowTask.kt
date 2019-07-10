package org.nypl.simplified.books.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.SettableFuture
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.Duration
import org.joda.time.Instant
import org.joda.time.LocalDateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptFulfillmentToken
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.DRMUnsupportedException
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMDeviceNotActive
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMFailure
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnparseableACSM
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnreadableACSM
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnsupportedContentType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.DRMError.DRMUnsupportedSystem
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.FeedCorrupted
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.FeedLoaderFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.FeedUnusable
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.HTTPRequestFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnsupportedAcquisition
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadInProgress
import org.nypl.simplified.books.book_registry.BookStatusHeld
import org.nypl.simplified.books.book_registry.BookStatusHeldReady
import org.nypl.simplified.books.book_registry.BookStatusHoldable
import org.nypl.simplified.books.book_registry.BookStatusRequestingDownload
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan
import org.nypl.simplified.books.book_registry.BookStatusType
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.bundled.api.BundledURIs
import org.nypl.simplified.books.controller.AdobeDRMExtensions.AdobeDRMFulfillmentException
import org.nypl.simplified.books.controller.BookBorrowTask.DownloadResult.DownloadCancelled
import org.nypl.simplified.books.controller.BookBorrowTask.DownloadResult.DownloadFailed
import org.nypl.simplified.books.controller.BookBorrowTask.DownloadResult.DownloadOK
import org.nypl.simplified.books.controller.api.BookBorrowExceptionBadBorrowFeed
import org.nypl.simplified.books.controller.api.BookBorrowExceptionDeviceNotActivated
import org.nypl.simplified.books.controller.api.BookBorrowExceptionNoCredentials
import org.nypl.simplified.books.controller.api.BookBorrowExceptionNoUsableAcquisition
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.books.book_registry.BookStatusDownloadResult
import org.nypl.simplified.books.controller.api.BookUnexpectedTypeException
import org.nypl.simplified.books.controller.api.BookUnsupportedTypeException
import org.nypl.simplified.downloader.core.DownloadListenerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPOAuthToken
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A book borrowing task.
 */

class BookBorrowTask(
  private val account: AccountType,
  private val acquisition: OPDSAcquisition,
  private val adobeDRM: AdobeAdeptExecutorType?,
  private val bookId: BookID,
  private val bookRegistry: BookRegistryType,
  private val borrowStrings: BookBorrowStringResourcesType,
  private val borrowTimeoutDuration: Duration = Duration.standardMinutes(1L),
  private val bundledContent: BundledContentResolverType,
  private val cacheDirectory: File,
  private val clock: () -> Instant,
  private val downloader: DownloaderType,
  private val downloads: ConcurrentHashMap<BookID, DownloadType>,
  private val downloadTimeoutDuration: Duration = Duration.standardMinutes(3L),
  private val entry: OPDSAcquisitionFeedEntry,
  private val feedLoader: FeedLoaderType) : Callable<BookStatusDownloadResult> {

  private val contentTypeACSM =
    "application/vnd.adobe.adept+xml"
  private val contentTypeSimplifiedBearerToken =
    "application/vnd.librarysimplified.bearer-token+json"
  private val contentTypeEPUB =
    "application/epub+zip"
  private val contentTypeOctetStream =
    "application/octet-stream"

  private val adobeACS =
    "Adobe ACS"

  private val logger = LoggerFactory.getLogger(BookBorrowTask::class.java)
  private val steps = TaskRecorder.create<BookStatusDownloadErrorDetails>()

  @Volatile
  private lateinit var databaseEntry: BookDatabaseEntryType

  @Volatile
  private var databaseEntryInitialized = false

  @Volatile
  private var adobeLoan: AdobeAdeptLoan? = null

  @Volatile
  private var downloadRunningTotal: Long = 0L

  @Volatile
  private var downloadTimeThen = this.clock.invoke()

  @Volatile
  private lateinit var fulfillURI: URI

  /**
   * The initial book value. Note that this is a synthesized value because we need to be
   * able to open the book database to get a real book value, and that database call might
   * fail. If the call fails, we have no "book" that we can refer to in order to publish a
   * "book download has failed" status for the book, so we use this fake book in that (rare)
   * situation.
   */

  private val bookInitial: Book =
    Book(
      id = this.bookId,
      account = this.account.id,
      cover = null,
      thumbnail = null,
      entry = this.entry,
      formats = listOf())

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}] ${message}", this.bookId.brief(), *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}] ${message}", this.bookId.brief(), *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}] ${message}", this.bookId.brief(), *arguments)

  @Throws(Exception::class)
  override fun call(): BookStatusDownloadResult {
    return try {
      this.debug("borrowing will time out after {} seconds",
        this.borrowTimeoutDuration.standardSeconds)
      this.debug("downloading will time out after {} seconds",
        this.downloadTimeoutDuration.standardSeconds)

      this.steps.beginNewStep(this.borrowStrings.borrowStarted)

      /*
       * Set up the book database.
       */

      this.createBookDatabaseEntry()

      /*
       * If the requested URI appears to refer to bundled content, serve the book from there.
       */

      if (BundledURIs.isBundledURI(this.acquisition.uri)) {
        this.runAcquisitionBundled()
        return BookStatusDownloadResult(this.steps.finish())
      }

      /*
       * Otherwise, do whatever is required for the acquisition.
       */

      when (val type = this.acquisition.relation) {
        ACQUISITION_BORROW -> {
          this.debug("acquisition type is {}, performing borrow", type)
          this.runAcquisitionBorrow()
          BookStatusDownloadResult(this.steps.finish())
        }
        ACQUISITION_GENERIC -> {
          this.debug("acquisition type is {}, performing generic procedure", type)
          this.runAcquisitionFulfill(this.entry)
          BookStatusDownloadResult(this.steps.finish())
        }
        ACQUISITION_OPEN_ACCESS -> {
          this.debug("acquisition type is {}, performing fulfillment", type)
          this.runAcquisitionFulfill(this.entry)
          BookStatusDownloadResult(this.steps.finish())
        }

        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {
          this.debug("acquisition type is {}, cannot continue!", type)
          val exception = UnsupportedOperationException()
          this.steps.currentStepFailed(
            message = this.borrowStrings.borrowBookUnsupportedAcquisition(type),
            errorValue = UnsupportedAcquisition(type),
            exception = exception)
          throw exception
        }
      }
    } catch (e: Throwable) {
      this.error("borrow failed: ", e)

      val step = this.steps.currentStep()!!
      if (step.exception == null) {
        this.steps.currentStepFailed(
          message = this.pickUsableMessage(step.resolution, e),
          errorValue = step.errorValue,
          exception = e)
      }

      val result = BookStatusDownloadResult(this.steps.finish())
      this.publishBookStatus(BookStatusDownloadFailed(this.bookId, result, Option.none()))
      result
    } finally {
      this.debug("finished")
    }
  }

  /*
   * Create a new book database entry and publish the status of the book.
   */

  private fun createBookDatabaseEntry() {
    this.steps.beginNewStep(this.borrowStrings.borrowBookDatabaseCreateOrUpdate)

    try {
      this.debug("setting up book database entry")
      val database = this.account.bookDatabase
      this.databaseEntry = database.createOrUpdate(this.bookId, this.entry)
      this.databaseEntryInitialized = true
      this.publishBookStatus(BookStatusRequestingLoan(
        this.bookId, this.borrowStrings.borrowBookDatabaseCreateOrUpdate))
      this.steps.currentStepSucceeded(this.borrowStrings.borrowBookDatabaseUpdated)
    } catch (e: Exception) {
      this.error("failed to set up book database entry: ", e)
      this.steps.currentStepFailed(this.borrowStrings.borrowBookDatabaseFailed, null, e)
      throw e
    }
  }

  private fun pickUsableMessage(message: String, e: Throwable): String {
    val exMessage = e.message
    return if (message.isEmpty()) {
      if (exMessage != null) {
        exMessage
      } else {
        e.javaClass.simpleName
      }
    } else {
      message
    }
  }

  /**
   * Hit a "borrow" link, read the resulting feed, download the book if it is
   * available.
   */

  private fun runAcquisitionBorrow() {
    this.debug("runAcquisitionBorrow")

    val feedEntry = this.runAcquisitionBorrowGetFeedEntry()
    this.runAcquisitionBorrowForOPDSEntry(feedEntry)
  }

  /**
   * Grab the feed for the borrow link.
   */

  private fun runAcquisitionBorrowGetFeedEntry(): FeedEntryOPDS {
    this.steps.beginNewStep(this.borrowStrings.borrowBookGetFeedEntry)

    val httpAuth = this.createHttpAuthIfRequired()
    this.debug("fetching item feed: {}", this.acquisition.uri)
    this.publishBookStatus(BookStatusRequestingLoan(this.bookId, this.borrowStrings.borrowBookGetFeedEntry))

    val feedResult =
      try {
        this.feedLoader.fetchURIRefreshing(this.acquisition.uri, httpAuth, "PUT")
          .get(this.borrowTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
      } catch (e: Exception) {
        this.error("feed loader raised exception: ", e)
        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookFeedLoadingFailed,
          errorValue = FeedLoaderFailed(null, e),
          exception = e)
        throw e
      }

    return when (feedResult) {
      is FeedLoaderResult.FeedLoaderSuccess -> {
        when (val resultFeed = feedResult.feed) {
          is Feed.FeedWithoutGroups -> {
            val entries =
              this.checkFeedHasEntries(this.acquisition.uri, resultFeed.entriesInOrder)

            when (val feedEntry = entries[0]) {
              is FeedEntryCorrupt -> {
                this.error("unexpectedly received corrupt feed entry")
                this.steps.currentStepFailed(
                  message = this.borrowStrings.borrowBookBadBorrowFeed,
                  errorValue = FeedCorrupted(feedEntry.error),
                  exception = feedEntry.error)
                throw BookBorrowExceptionBadBorrowFeed(feedEntry.error)
              }
              is FeedEntryOPDS -> feedEntry
            }
          }

          is Feed.FeedWithGroups -> {
            val groups =
              this.checkFeedHasGroups(this.acquisition.uri, resultFeed.feedGroupsInOrder)
            val entries =
              this.checkFeedHasEntries(this.acquisition.uri, groups[0].groupEntries)

            when (val feedEntry = entries[0]) {
              is FeedEntryCorrupt -> {
                this.error("unexpectedly received corrupt feed entry")
                this.steps.currentStepFailed(
                  message = this.borrowStrings.borrowBookBadBorrowFeed,
                  errorValue = FeedCorrupted(feedEntry.error),
                  exception = feedEntry.error)
                throw BookBorrowExceptionBadBorrowFeed(feedEntry.error)
              }
              is FeedEntryOPDS ->
                feedEntry
            }
          }
        }
      }

      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral -> {
        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookFeedLoadingFailed,
          errorValue = FeedLoaderFailed(feedResult.problemReport, feedResult.exception),
          exception = feedResult.exception)
        throw feedResult.exception
      }

      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication -> {
        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookFeedLoadingFailed,
          errorValue = FeedLoaderFailed(feedResult.problemReport, feedResult.exception),
          exception = feedResult.exception)
        throw feedResult.exception
      }
    }
  }

  /**
   * Check that a feed has at least one group.
   */

  private fun checkFeedHasGroups(
    uri: URI,
    groups: List<FeedGroup>): List<FeedGroup> {
    if (groups.isEmpty()) {
      this.error("unexpectedly received feed with zero groups")
      val exception = BookBorrowExceptionBadBorrowFeed(IOException("No groups in feed: $uri"))
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookBadBorrowFeed,
        errorValue = FeedUnusable,
        exception = exception)
      throw exception
    }
    return groups
  }

  /**
   * Check that a feed has at least one entry.
   */

  private fun checkFeedHasEntries(
    uri: URI,
    entries: List<FeedEntry>): List<FeedEntry> {
    if (entries.isEmpty()) {
      this.error("unexpectedly received feed with no entries")
      val exception = BookBorrowExceptionBadBorrowFeed(IOException("No entries in feed: $uri"))
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookBadBorrowFeed,
        errorValue = FeedUnusable,
        exception = exception)
      throw exception
    }
    return entries
  }

  /**
   * Complete borrowing given an OPDS feed entry.
   */

  private fun runAcquisitionBorrowForOPDSEntry(feedEntry: FeedEntryOPDS) {
    val availability = feedEntry.feedEntry.availability
    this.steps.beginNewStep(this.borrowStrings.borrowBookBorrowForAvailability(availability))

    this.debug("received OPDS feed entry")
    this.debug("book availability is {}", availability)

    /*
     * Update the database.
     */

    this.debug("saving state to database")
    this.databaseEntry.writeOPDSEntry(feedEntry.feedEntry)

    /*
     * Then, work out what to do based on the latest availability data.
     * If the book is loaned, attempt to download it. If it is held, notify
     * the user.
     */

    this.debug("continuing borrow based on availability")

    val wantFulfill = availability.matchAvailability(
      object : OPDSAvailabilityMatcherType<Boolean, UnreachableCodeException> {

        /**
         * If the book is held but is ready for download, just notify
         * the user of this fact by setting the status.
         */

        override fun onHeldReady(a: OPDSAvailabilityHeldReady): Boolean {
          this@BookBorrowTask.debug("book is held but is ready, nothing more to do")
          this@BookBorrowTask.publishBookStatus(
            BookStatusHeldReady(this@BookBorrowTask.bookId, a.endDate, a.revoke.isSome))
          return false
        }

        /**
         * If the book is held, just notify the user of this fact by
         * setting the status.
         */

        override fun onHeld(a: OPDSAvailabilityHeld): Boolean {
          this@BookBorrowTask.debug("book is held, nothing more to do")
          this@BookBorrowTask.publishBookStatus(BookStatusHeld(
            id = this@BookBorrowTask.bookId,
            queuePosition = a.position,
            startDate = a.startDate,
            endDate = a.endDate,
            isRevocable = a.revoke.isSome))
          return false
        }

        /**
         * If the book is available to be placed on hold, set the
         * appropriate status.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onHoldable(a: OPDSAvailabilityHoldable): Boolean {
          this@BookBorrowTask.debug("book is holdable, cannot continue!")
          this@BookBorrowTask.publishBookStatus(BookStatusHoldable(this@BookBorrowTask.bookId))
          return false
        }

        /**
         * If the book claims to be only "loanable", then something is
         * definitely wrong.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onLoanable(a: OPDSAvailabilityLoanable): Boolean {
          this@BookBorrowTask.debug("book is loanable, this is a server bug!")
          return false
        }

        /**
         * If the book is "loaned", then attempt to fulfill the book.
         */

        override fun onLoaned(a: OPDSAvailabilityLoaned): Boolean {
          this@BookBorrowTask.debug("book is loaned, fulfilling")
          this@BookBorrowTask.publishBookStatus(
            BookStatusRequestingDownload(
              id = this@BookBorrowTask.bookId,
              detailMessage = "",
              loanEndDate = a.endDate))
          return java.lang.Boolean.TRUE
        }

        /**
         * If the book is "open-access", then attempt to fulfill the
         * book.
         */

        override fun onOpenAccess(a: OPDSAvailabilityOpenAccess): Boolean {
          this@BookBorrowTask.debug("book is open access, fulfilling")
          this@BookBorrowTask.publishBookStatus(
            BookStatusRequestingDownload(
              id = this@BookBorrowTask.bookId,
              detailMessage = "",
              loanEndDate = Option.none()))
          return java.lang.Boolean.TRUE
        }

        /**
         * The server cannot return a "revoked" representation. Reaching
         * this code indicates a serious bug in the application.
         */

        override fun onRevoked(a: OPDSAvailabilityRevoked): Boolean {
          throw UnreachableCodeException()
        }
      })

    return if (wantFulfill) {
      this.runAcquisitionFulfill(feedEntry.feedEntry)
    } else {
      val exception = IllegalStateException()
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookBorrowAvailabilityInappropriate(availability),
        errorValue = null,
        exception = exception)
      throw exception
    }
  }

  /**
   * Fulfill a book by hitting the generic or open access links.
   */

  private fun runAcquisitionFulfill(ee: OPDSAcquisitionFeedEntry) {
    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfill)

    this.debug("fulfilling book")

    for (ea in ee.acquisitions) {
      when (ea.relation) {
        ACQUISITION_GENERIC,
        ACQUISITION_OPEN_ACCESS ->
          return this.runAcquisitionFulfillDoDownload(ea, this.createHttpAuthIfRequired())
        ACQUISITION_BORROW,
        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {

        }
      }
    }

    val exception = BookBorrowExceptionNoUsableAcquisition()
    this.steps.currentStepFailed(
      message = this.borrowStrings.borrowBookFulfillNoUsableAcquisitions,
      errorValue = null,
      exception = exception)
    throw exception
  }

  /**
   * The result of a download attempt.
   */

  private sealed class DownloadResult {

    /**
     * Downloading succeeded.
     */

    data class DownloadOK(
      val file: File)
      : DownloadResult()

    /**
     * Downloading failed.
     */

    data class DownloadFailed(
      val status: Int,
      val problemReport: OptionType<HTTPProblemReport>,
      val exception: OptionType<Throwable>)
      : DownloadResult()

    /**
     * Downloading was cancelled.
     */

    object DownloadCancelled
      : DownloadResult()
  }

  /**
   * A download listener that reports progress and sets the value of a future on completion
   * or errors.
   */

  private class DownloadListener(
    val downloadFuture: SettableFuture<DownloadResult>,
    val onDownloadProgress: (Long, Long, unconditional: Boolean) -> Unit) : DownloadListenerType {

    override fun onDownloadStarted(
      download: DownloadType,
      expectedTotal: Long) {
      this.onDownloadProgress.invoke(0L, expectedTotal, true)
    }

    override fun onDownloadDataReceived(
      download: DownloadType,
      runningTotal: Long,
      expectedTotal: Long) {
      this.onDownloadProgress.invoke(runningTotal, expectedTotal, false)
    }

    override fun onDownloadCancelled(d: DownloadType) {
      this.downloadFuture.set(DownloadCancelled)
    }

    override fun onDownloadFailed(
      download: DownloadType,
      status: Int,
      runningTotal: Long,
      problemReport: OptionType<HTTPProblemReport>,
      exception: OptionType<Throwable>) {
      this.downloadFuture.set(DownloadFailed(status, problemReport, exception))
    }

    override fun onDownloadCompleted(
      download: DownloadType,
      file: File) {
      this.downloadFuture.set(DownloadOK(file))
    }
  }

  private fun runAcquisitionFulfillDoDownload(
    acquisition: OPDSAcquisition,
    httpAuth: OptionType<HTTPAuthType>) {

    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillDownload)

    this.fulfillURI = acquisition.uri

    /*
     * Point the downloader at the acquisition link. The result will be an
     * EPUB, ACSM file, or Simplified bearer token. ACSM files have to be
     * "fulfilled" after downloading by passing them to the Adobe DRM
     * connector. Bearer token documents need an additional request to
     * actually get the book in question.
     */

    val downloadFuture =
      SettableFuture.create<DownloadResult>()
    val downloadListener =
      DownloadListener(downloadFuture) { runningTotal, expectedTotal, unconditional ->
        this.downloadDataReceived(
          detailMessage = this.borrowStrings.borrowBookFulfillDownload,
          runningTotal = runningTotal,
          expectedTotal = expectedTotal,
          unconditional = unconditional)
      }

    val download =
      this.downloader.download(acquisition.uri, httpAuth, downloadListener)

    this.downloads[this.bookId] = download

    val result = try {
      downloadFuture.get(this.downloadTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
    } catch (ex: TimeoutException) {
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookFulfillTimedOut,
        errorValue = null,
        exception = ex)
      download.cancel()
      downloadFuture.cancel(true)
      throw IOException("Timed out", ex)
    } finally {
      this.downloads.remove(this.bookId)
    }

    val file = this.fileFromDownloadResult(result)
    this.debug("download {} completed for {}", download, file)
    val contentType = download.contentType
    this.debug("content type is {}", contentType)

    this.steps.currentStepSucceeded(
      this.borrowStrings.borrowBookFulfillDownloaded(file, contentType))

    return when (contentType) {
      this.contentTypeACSM ->
        this.runFulfillACSM(file)
      this.contentTypeSimplifiedBearerToken ->
        this.runFulfillSimplifiedBearerToken(acquisition, file)
      else ->
        this.saveFinalContent(
          file = file,
          expectedContentTypes = acquisition.availableFinalContentTypes(),
          receivedContentType = contentType)
    }
  }

  private fun fileFromDownloadResult(result: DownloadResult): File {
    return when (result) {
      is DownloadOK ->
        result.file

      is DownloadFailed -> {
        val exception = IOException()
        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookFulfillDownloadFailed,
          errorValue = HTTPRequestFailed(
            status = result.status,
            errorReport = this.someOrNull(result.problemReport)
          ),
          exception = exception)
        throw exception
      }

      DownloadCancelled -> {
        this.steps.currentStepFailed(this.borrowStrings.borrowBookFulfillCancelled)
        throw CancellationException()
      }
    }
  }

  private fun <T> someOrNull(option: OptionType<T>): T? {
    return if (option is Some<T>) {
      option.get()
    } else {
      null
    }
  }

  private fun saveFinalContent(
    file: File,
    expectedContentTypes: Set<String>,
    receivedContentType: String) {

    this.steps.beginNewStep(
      this.borrowStrings.borrowBookSaving(receivedContentType, expectedContentTypes))

    this.debug(
      "saving content {} (expected one of {}, received {})",
      file,
      expectedContentTypes,
      receivedContentType)

    this.debug("saving adobe rights {}", this.adobeLoan)
    this.debug("saving fulfill URI  {}", this.fulfillURI)

    val handleContentType =
      this.checkExpectedContentType(expectedContentTypes, receivedContentType)
    val formatHandle =
      this.databaseEntry.findFormatHandleForContentType(handleContentType)

    fun updateStatus() {
      this.publishBookStatus(BookStatus.fromBook(this.databaseEntry.book))
    }

    return if (formatHandle != null) {
      when (formatHandle) {
        is BookDatabaseEntryFormatHandleEPUB -> {
          formatHandle.copyInBook(file)
          formatHandle.setAdobeRightsInformation(this.adobeLoan)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandlePDF -> {
          formatHandle.copyInBook(file)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandleAudioBook -> {
          formatHandle.copyInManifestAndURI(file, this.fulfillURI)
          updateStatus()
        }
      }
    } else {
      this.error("database entry does not have a format handle for {}", handleContentType)
      val exception = BookUnsupportedTypeException(handleContentType)
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookSavingCheckingContentTypeUnacceptable,
        errorValue = null,
        exception = exception)
      throw exception
    }
  }

  /*
   * If we expect a specific content type, but the server actually delivers application/octet-stream,
   * then assume that the server delivered the expected type. Otherwise, check that the received
   * type matches the expected type.
   */

  private fun checkExpectedContentType(
    expectedContentTypes: Set<String>,
    receivedContentType: String): String {

    this.steps.beginNewStep(
      this.borrowStrings.borrowBookSavingCheckingContentType(
        receivedContentType, expectedContentTypes))

    Preconditions.checkArgument(
      !expectedContentTypes.isEmpty(),
      "At least one expected content type")

    return when (receivedContentType) {
      this.contentTypeOctetStream -> {
        this.debug("expected one of {} but received {} (acceptable)",
          expectedContentTypes,
          receivedContentType)

        this.steps.currentStepSucceeded(this.borrowStrings.borrowBookSavingCheckingContentTypeOK)
        expectedContentTypes.first()
      }

      else -> {
        if (expectedContentTypes.contains(receivedContentType)) {
          return receivedContentType
        }

        this.debug(
          "expected {} but received {} (unacceptable)",
          expectedContentTypes,
          receivedContentType)

        val exception =
          BookUnexpectedTypeException(
            message =
            StringBuilder("Unexpected content type\n")
              .append("  Expected: One of ")
              .append(expectedContentTypes)
              .append('\n')
              .append("  Received: ")
              .append(receivedContentType)
              .append('\n')
              .toString(),
            expected = expectedContentTypes,
            received = receivedContentType)

        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookSavingCheckingContentTypeUnacceptable,
          errorValue = null,
          exception = exception)

        throw exception
      }
    }
  }

  /**
   * Fulfill the given ACSM file, if Adobe DRM is supported. Otherwise, fail.
   */

  private fun runFulfillACSM(file: File) {
    this.debug("fulfilling ACSM file")

    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillACSM)

    /*
     * The ACSM file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. The Adobe library will then take up
     * to roughly ten seconds to start fulfilling the ACSM. This call
     * effectively sets the download progress bar to 0% so that it doesn't look
     * as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(
      detailMessage = this.borrowStrings.borrowBookFulfillACSM,
      runningTotal = 0L,
      expectedTotal = 100L,
      unconditional = true)

    return if (this.adobeDRM != null) {
      this.debug("DRM support is available, using DRM connector")
      this.runFulfillACSMWithConnector(this.adobeDRM, file)
    } else {
      this.debug("DRM support is unavailable, cannot continue!")
      val ex = DRMUnsupportedException("DRM support is not available")
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookFulfillDRMNotSupported,
        errorValue = DRMUnsupportedSystem(this.adobeACS),
        exception = ex)
      throw ex
    }
  }

  /**
   * Execute all of the busy work required to set up the DRM connector, and then make the
   * actual DRM calls to download the file.
   */

  private fun runFulfillACSMWithConnector(
    adobe: AdobeAdeptExecutorType,
    file: File
  ) {
    val credentials =
      this.runFulfillACSMWithConnectorGetCredentials()
    val acsmBytes =
      this.runFulfillACSMWithConnectorReadACSM(file)
    val parsed =
      this.runFulfillACSMWithConnectorParse(acsmBytes)

    this.runFulfillACSMWithConnectorCheckContentType(parsed)

    val fulfillment =
      this.runFulfillACSMWithConnectorDoDownload(adobe, acsmBytes, credentials)

    try {
      this.adobeLoan = fulfillment.loan
      this.saveFinalContent(
        file = fulfillment.file,
        expectedContentTypes = BookFormats.epubMimeTypes(),
        receivedContentType = this.contentTypeEPUB)
    } finally {
      try {
        FileUtilities.fileDelete(fulfillment.file)
      } catch (e: Exception) {
        this.logger.debug("ignoring failed deletion of fulfillment file: ", e)
      }
    }
  }

  /**
   * Do the actual DRM calls needed to download a file from an ACSM.
   */

  private fun runFulfillACSMWithConnectorDoDownload(
    adobe: AdobeAdeptExecutorType,
    acsmBytes: ByteArray,
    credentials: AccountAuthenticationAdobePostActivationCredentials
  ): AdobeDRMExtensions.Fulfillment {
    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillACSMConnector)

    this.downloadDataReceived(
      detailMessage = this.borrowStrings.borrowBookFulfillACSMConnector,
      runningTotal = 0,
      expectedTotal = 100,
      unconditional = true)

    val outputFile =
      File.createTempFile("ADOBE-DRM", "data", this.cacheDirectory)

    val future =
      AdobeDRMExtensions.fulfill(
        adobe,
        { message -> this.error(message) },
        { message -> this.debug(message) },
        { connector -> this.runFulfillACSMWithConnectorCreateFakeDownload(connector) },
        { progress ->
          this.downloadDataReceived(
            detailMessage = this.borrowStrings.borrowBookFulfillACSMConnector,
            runningTotal = progress.toLong(),
            expectedTotal = 100,
            unconditional = false)
        },
        outputFile,
        acsmBytes,
        credentials.userID)

    val fulfillment =
      try {
        val seconds = this.downloadTimeoutDuration.standardSeconds
        this.logger.debug("waiting for fulfillment for {} seconds", seconds)
        future.get(seconds, TimeUnit.SECONDS)
      } catch (e: TimeoutException) {
        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookFulfillTimedOut,
          errorValue = null,
          exception = e)
        this.downloads[this.bookId]?.cancel()
        throw IOException("Timed out", e)
      } catch (e: ExecutionException) {
        throw when (val cause = e.cause!!) {
          is CancellationException -> {
            this.steps.currentStepFailed(
              message = this.borrowStrings.borrowBookFulfillCancelled,
              errorValue = null,
              exception = cause)
            cause
          }
          is AdobeDRMFulfillmentException -> {
            this.steps.currentStepFailed(
              message = this.borrowStrings.borrowBookFulfillACSMConnectorFailed(cause.errorCode),
              errorValue = DRMFailure(this.adobeACS, cause.errorCode),
              exception = cause)
            cause
          }
          else -> {
            this.steps.currentStepFailed(
              message = this.borrowStrings.borrowBookFulfillACSMFailed,
              errorValue = null,
              exception = cause)
            cause
          }
        }
      } catch (e: Throwable) {
        this.steps.currentStepFailed(
          message = this.borrowStrings.borrowBookFulfillACSMFailed,
          errorValue = null,
          exception = e)
        throw e
      }

    this.steps.currentStepSucceeded(this.borrowStrings.borrowBookFulfillACSMConnectorOK)
    return fulfillment
  }

  /**
   * Retrieve the post-activation device credentials. These can only exist if the device
   * has been activated.
   */

  private fun runFulfillACSMWithConnectorGetCredentials(): AccountAuthenticationAdobePostActivationCredentials {
    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillACSMGettingDeviceCredentials)

    val credentials =
      this.someOrNull(this.getRequiredAccountCredentials().adobePostActivationCredentials())

    if (credentials == null) {
      val exception = BookBorrowExceptionDeviceNotActivated()
      this.steps.currentStepFailed(
        this.borrowStrings.borrowBookFulfillACSMGettingDeviceCredentialsNotActivated,
        errorValue = DRMDeviceNotActive(this.adobeACS),
        exception = exception)
      throw exception
    }

    this.steps.currentStepSucceeded(this.borrowStrings.borrowBookFulfillACSMGettingDeviceCredentialsOK)
    return credentials
  }

  /**
   * Read the ACSM file.
   */

  private fun runFulfillACSMWithConnectorReadACSM(file: File): ByteArray {
    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillACSMRead)

    try {
      return FileUtilities.fileReadBytes(file)
    } catch (e: Exception) {
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookFulfillACSMReadFailed,
        errorValue = DRMUnreadableACSM(this.adobeACS),
        exception = e)
      throw e
    }
  }

  /**
   * Create a fake download that cancels the Adobe download via
   * the net provider. There can only be one Adobe download in progress
   * at a time (the {@link AdobeAdeptExecutorType} interface
   * guarantees this), so the download must refer to the current one.
   */

  private fun runFulfillACSMWithConnectorCreateFakeDownload(connector: AdobeAdeptConnectorType) {
    this.downloads[this.bookId] = object : DownloadType {
      override fun cancel() {
        val net = connector.netProvider
        net.cancel()
      }

      override fun getContentType(): String {
        return this@BookBorrowTask.contentTypeOctetStream
      }
    }
  }

  /**
   * Check that we actually support DRMd content of the given type.
   */

  private fun runFulfillACSMWithConnectorCheckContentType(parsed: AdobeAdeptFulfillmentToken) {
    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillACSMCheckContentType)

    val contentType = parsed.format
    if (this.contentTypeEPUB != contentType) {
      val exception = BookUnsupportedTypeException(contentType)
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookFulfillACSMUnsupportedContentType,
        errorValue = DRMUnsupportedContentType(this.adobeACS, contentType),
        exception = exception)
      throw exception
    }

    this.steps.currentStepSucceeded(
      this.borrowStrings.borrowBookFulfillACSMCheckContentTypeOK(contentType))
  }

  /**
   * Parse a series of bytes that are expected to comprise an ACSM file.
   */

  private fun runFulfillACSMWithConnectorParse(acsmBytes: ByteArray): AdobeAdeptFulfillmentToken {
    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillACSMParse)

    val parsed = try {
      AdobeAdeptFulfillmentToken.parseFromBytes(acsmBytes)
    } catch (e: Exception) {
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookFulfillACSMParseFailed,
        errorValue = DRMUnparseableACSM(this.adobeACS),
        exception = e)
      throw e
    }
    return parsed
  }

  private fun runFulfillSimplifiedBearerToken(
    acquisition: OPDSAcquisition,
    file: File) {
    this.debug("fulfilling Simplified bearer token file")

    this.steps.beginNewStep(this.borrowStrings.borrowBookFulfillBearerToken)

    /*
     * The bearer token file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. This call effectively sets the download progress bar
     * to 0% so that it doesn't look as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(
      detailMessage = this.borrowStrings.borrowBookFulfillBearerToken,
      runningTotal = 0L,
      expectedTotal = 100L,
      unconditional = true)

    val token = try {
      SimplifiedBearerTokenJSON.deserializeFromFile(ObjectMapper(), LocalDateTime.now(), file)
    } catch (ex: Exception) {
      this.error("failed to parse bearer token: {}: ", acquisition.uri, ex)
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookFulfillUnparseableBearerToken,
        errorValue = null,
        exception = ex)
      throw ex
    }

    this.steps.currentStepSucceeded(this.borrowStrings.borrowBookFulfillBearerTokenOK)

    val nextAcquisition =
      OPDSAcquisition(
        ACQUISITION_GENERIC,
        token.location,
        acquisition.type,
        acquisition.indirectAcquisitions)

    val auth = HTTPAuthOAuth.create(HTTPOAuthToken.create(token.accessToken))
    this.runAcquisitionFulfillDoDownload(nextAcquisition, Option.some(auth))
  }

  /**
   * If the account requires credentials, create HTTP auth details. If no credentials
   * are provided, throw an exception.
   */

  private fun createHttpAuthIfRequired(): OptionType<HTTPAuthType> {
    return if (this.account.requiresCredentials) {
      Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(this.getRequiredAccountCredentials()))
    } else {
      Option.none<HTTPAuthType>()
    }
  }

  /**
   * Assume that account credentials are required and fetch them. If they're not present, fail
   * loudly.
   */

  private fun getRequiredAccountCredentials(): AccountAuthenticationCredentials {
    val loginState = this.account.loginState
    val credentials = loginState.credentials
    if (credentials != null) {
      return credentials
    } else {
      this.error("borrowing requires credentials, but none are available")
      throw BookBorrowExceptionNoCredentials()
    }
  }

  /**
   * Copy data out of the bundled resources.
   */

  private fun runAcquisitionBundled() {
    this.debug("acquisition is bundled")
    this.steps.beginNewStep(this.borrowStrings.borrowBookBundledCopy)
    this.publishBookStatus(BookStatusRequestingLoan(this.bookId, this.borrowStrings.borrowBookBundledCopy))

    this.fulfillURI = this.acquisition.uri
    val file = this.databaseEntry.temporaryFile()
    val buffer = ByteArray(2048)

    try {
      return FileOutputStream(file).use { output ->
        this.bundledContent.resolve(this.acquisition.uri).use { stream ->
          val size = stream.available().toLong()
          var consumed = 0L
          this.downloadDataReceived(
            detailMessage = this.borrowStrings.borrowBookBundledCopy,
            runningTotal = consumed,
            expectedTotal = size,
            unconditional = true)

          while (true) {
            val r = stream.read(buffer)
            if (r == -1) {
              break
            }
            consumed += r.toLong()
            output.write(buffer, 0, r)

            this.downloadDataReceived(
              detailMessage = this.borrowStrings.borrowBookBundledCopy,
              runningTotal = consumed,
              expectedTotal = size,
              unconditional = false)
          }
          output.flush()
        }

        /*
         * XXX: This implicitly encodes an assumption that only EPUB files will
         * ever be bundled with the app.
         */

        this.saveFinalContent(
          file = file,
          expectedContentTypes = BookFormats.epubMimeTypes(),
          receivedContentType = this.contentTypeOctetStream)

        this.publishBookStatus(BookStatus.fromBook(this.databaseEntry.book))
      }
    } catch (e: Exception) {
      this.steps.currentStepFailed(
        message = this.borrowStrings.borrowBookBundledCopyFailed,
        errorValue = null,
        exception = e)
      FileUtilities.fileDelete(file)
      throw e
    }
  }

  private fun publishBookStatus(status: BookStatusType) {
    val book =
      if (this.databaseEntryInitialized) {
        this.databaseEntry.book
      } else {
        this.warn("publishing status using a fake book")
        this.bookInitial
      }
    this.bookRegistry.update(BookWithStatus.create(book, status))
  }

  private fun downloadDataReceived(
    detailMessage: String,
    runningTotal: Long,
    expectedTotal: Long,
    unconditional: Boolean) {

    /*
     * Because "data received" updates happen at such a huge rate, we want
     * to ensure that updates to the book status are rate limited to avoid
     * overwhelming the UI. Book updates are limited to a rate of ten per
     * second.
     */

    val timeNow = this.clock.invoke()
    val period = Period(this.downloadTimeThen, timeNow, PeriodType.millis())
    if (period.millis >= 100 || unconditional) {
      val status =
        BookStatusDownloadInProgress(
          id = this.bookId,
          detailMessage = detailMessage,
          currentTotalBytes = runningTotal,
          expectedTotalBytes = expectedTotal,
          loanEndDate = Option.none())
      this.bookRegistry.update(BookWithStatus.create(this.databaseEntry.book, status))
      this.downloadRunningTotal = runningTotal
    }
  }
}
