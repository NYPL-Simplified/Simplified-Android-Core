package org.nypl.simplified.books.controller

import android.net.Uri
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.SettableFuture
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.Duration
import org.joda.time.Instant
import org.joda.time.LocalDateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptFulfillmentToken
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.DRMUnsupportedException
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions.AdobeDRMFulfillmentException
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.audio.AudioBookCredentials
import org.nypl.simplified.books.audio.AudioBookManifestRequest
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.BookDatabaseFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.BundledCopyFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.ContentCopyFailed
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
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.TimedOut
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnexpectedException
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnparseableBearerToken
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnsupportedAcquisition
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnsupportedType
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails.UnusableAcquisitions
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled.api.BundledURIs
import org.nypl.simplified.books.controller.BookBorrowTask.DownloadResult.DownloadCancelled
import org.nypl.simplified.books.controller.BookBorrowTask.DownloadResult.DownloadFailed
import org.nypl.simplified.books.controller.BookBorrowTask.DownloadResult.DownloadOK
import org.nypl.simplified.books.controller.BookCoverFetchTask.Type
import org.nypl.simplified.books.controller.api.BookBorrowExceptionBadBorrowFeed
import org.nypl.simplified.books.controller.api.BookBorrowExceptionDeviceNotActivated
import org.nypl.simplified.books.controller.api.BookBorrowExceptionNoCredentials
import org.nypl.simplified.books.controller.api.BookBorrowExceptionNoUsableAcquisition
import org.nypl.simplified.books.controller.api.BookUnexpectedTypeException
import org.nypl.simplified.books.controller.api.BookUnsupportedTypeException
import org.nypl.simplified.downloader.core.DownloadListenerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPHasProblemReportType
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
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
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
  private val services: BookTaskRequiredServices,
  private val accountId: AccountID,
  private val acquisition: OPDSAcquisition,
  private val bookId: BookID,
  private val borrowTimeoutDuration: Duration = Duration.standardMinutes(1L),
  private val cacheDirectory: File,
  private val downloads: ConcurrentHashMap<BookID, DownloadType>,
  private val downloadTimeoutDuration: Duration = Duration.standardMinutes(3L),
  private val entry: OPDSAcquisitionFeedEntry
) : Callable<TaskResult<BookStatusDownloadErrorDetails, Unit>> {

  private val contentTypeACSM =
    MIMEParser.parseRaisingException("application/vnd.adobe.adept+xml")
  private val contentTypeSimplifiedBearerToken =
    MIMEParser.parseRaisingException("application/vnd.librarysimplified.bearer-token+json")
  private val contentTypeEPUB =
    MIMEParser.parseRaisingException("application/epub+zip")
  private val contentTypeOctetStream =
    MIMEParser.parseRaisingException("application/octet-stream")
  private val contentTypeJson =
    MIMEParser.parseRaisingException("application/json")

  private val adobeACS =
    "Adobe ACS"

  private val logger = LoggerFactory.getLogger(BookBorrowTask::class.java)
  private val steps = TaskRecorder.create<BookStatusDownloadErrorDetails>()

  @Volatile
  private lateinit var databaseEntry: BookDatabaseEntryType

  @Volatile
  private lateinit var account: AccountType

  @Volatile
  private var databaseEntryInitialized = false

  @Volatile
  private var adobeLoan: AdobeAdeptLoan? = null

  @Volatile
  private var downloadRunningTotal: Long = 0L

  @Volatile
  private var downloadTimeThen = Instant(0L)

  @Volatile
  private lateinit var fulfillURI: URI

  @Volatile
  private var drmKind = BookDRMKind.NONE

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
      account = this.accountId,
      cover = null,
      thumbnail = null,
      entry = this.entry,
      formats = listOf()
    )

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}] $message", this.bookId.brief(), *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}] $message", this.bookId.brief(), *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}] $message", this.bookId.brief(), *arguments)

  @Throws(Exception::class)
  override fun call(): TaskResult<BookStatusDownloadErrorDetails, Unit> {
    return try {
      this.steps.beginNewStep(this.services.borrowStrings.borrowStarted)
      this.downloadTimeThen = this.services.clock.clockNow()

      this.debug(
        "borrowing will time out after {} seconds",
        this.borrowTimeoutDuration.standardSeconds
      )
      this.debug(
        "downloading will time out after {} seconds",
        this.downloadTimeoutDuration.standardSeconds
      )

      /*
       * Locate the account in the current profile.
       */

      val foundAccount = try {
        val profile = this.services.profiles.currentProfileUnsafe()
        profile.account(this.accountId)
      } catch (e: Throwable) {
        this.logger.error("[{}]: failed to find account! ", this.bookId.brief(), e)

        val failure =
          TaskResult.fail<BookStatusDownloadErrorDetails, Unit>(
            description = this.services.borrowStrings.borrowStarted,
            resolution = this.services.borrowStrings.borrowBookUnexpectedException,
            errorValue = UnexpectedException(e)
          ) as TaskResult.Failure<BookStatusDownloadErrorDetails, Unit>

        this.services.bookRegistry.update(
          BookWithStatus(this.bookInitial, BookStatus.FailedLoan(this.bookId, failure))
        )

        return failure
      }

      this.account = foundAccount

      /*
       * Set up the book database.
       */

      this.createBookDatabaseEntry()

      /*
       * If the requested URI appears to refer to bundled content, serve the book from there.
       */

      if (BundledURIs.isBundledURI(this.acquisition.uri)) {
        this.runAcquisitionBundled()
        this.runFetchCover(this.entry, this.createHttpAuthIfRequired())
        return this.steps.finishSuccess(Unit)
      }

      /*
       * If the requested URI appears to be a content URI, serve the content from the resolver.
       */

      if (this.acquisition.uri.scheme == "content") {
        this.runAcquisitionContentURI()
        this.runFetchCover(this.entry, this.createHttpAuthIfRequired())
        return this.steps.finishSuccess(Unit)
      }

      /*
       * Otherwise, do whatever is required for the acquisition.
       */

      when (val type = this.acquisition.relation) {
        ACQUISITION_BORROW -> {
          this.debug("acquisition type is {}, performing borrow", type)
          this.runAcquisitionBorrow()
          return this.steps.finishSuccess(Unit)
        }
        ACQUISITION_GENERIC -> {
          this.debug("acquisition type is {}, performing generic procedure", type)
          this.runAcquisitionFulfill(this.entry)
          return this.steps.finishSuccess(Unit)
        }
        ACQUISITION_OPEN_ACCESS -> {
          this.debug("acquisition type is {}, performing fulfillment", type)
          this.runAcquisitionFulfill(this.entry)
          return this.steps.finishSuccess(Unit)
        }

        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {
          this.debug("acquisition type is {}, cannot continue!", type)
          val exception = UnsupportedOperationException()
          val message = this.services.borrowStrings.borrowBookUnsupportedAcquisition(type)
          this.steps.currentStepFailed(
            message = message,
            errorValue = UnsupportedAcquisition(
              message = message,
              type = type,
              attributes = this.currentAttributesWith(Pair("Acquisition type", type.toString()))
            ),
            exception = exception
          )
          throw exception
        }
      }
    } catch (e: Throwable) {
      this.error("borrow failed: ", e)

      this.steps.currentStepFailedAppending(
        this.services.borrowStrings.borrowBookUnexpectedException,
        UnexpectedException(e, this.currentAttributesWith()),
        e
      )

      val result = this.steps.finishFailure<Unit>()
      val status =
        if (this.databaseEntryInitialized) {
          BookStatus.fromBook(this.databaseEntry.book)
        } else {
          this.services.bookRegistry.bookStatusOrNull(this.bookId)
        }

      if (status is BookStatus.Loaned) {
        this.publishBookStatus(BookStatus.FailedDownload(this.bookId, result))
      } else {
        this.publishBookStatus(BookStatus.FailedLoan(this.bookId, result))
      }

      result
    } finally {
      this.debug("finished")
    }
  }

  private fun currentAttributesWith(attributes: Map<String, String>): Map<String, String> {
    val attrs = mutableMapOf<String, String>()
    for (entry in attributes.entries) {
      attrs[entry.key] = entry.value
    }
    attrs["Book"] = this.entry.title
    attrs["Author"] = this.entry.authorsCommaSeparated
    return attrs.toMap()
  }

  private fun currentAttributesWith(pairs: List<Pair<String, String>>): Map<String, String> =
    this.currentAttributesWith(pairs.toMap())

  private fun currentAttributesWith(vararg pairs: Pair<String, String>): Map<String, String> =
    this.currentAttributesWith(pairs.toList())

  /*
   * Create a new book database entry and publish the status of the book.
   */

  private fun createBookDatabaseEntry() {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookDatabaseCreateOrUpdate)

    try {
      this.debug("setting up book database entry")
      val database = this.account.bookDatabase
      this.databaseEntry = database.createOrUpdate(this.bookId, this.entry)
      this.databaseEntryInitialized = true

      this.publishBookStatus(
        BookStatus.RequestingLoan(
          id = this.bookId,
          detailMessage = this.services.borrowStrings.borrowBookDatabaseCreateOrUpdate
        )
      )
      this.steps.currentStepSucceeded(this.services.borrowStrings.borrowBookDatabaseUpdated)
    } catch (e: Exception) {
      this.error("failed to set up book database entry: ", e)
      val message = this.services.borrowStrings.borrowBookDatabaseFailed
      this.steps.currentStepFailed(
        message = message,
        errorValue = BookDatabaseFailed(message, this.currentAttributesWith()),
        exception = e
      )
      throw e
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
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookGetFeedEntry)

    val httpAuth = this.createHttpAuthIfRequired()
    this.debug("fetching item feed: {}", this.acquisition.uri)

    this.publishBookStatus(
      BookStatus.RequestingLoan(
        id = this.bookId,
        detailMessage = this.services.borrowStrings.borrowBookGetFeedEntry
      )
    )

    val feedResult =
      try {
        this.services.feedLoader.fetchURIRefreshing(
          this.accountId,
          this.acquisition.uri,
          httpAuth,
          "PUT"
        ).get(this.borrowTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
      } catch (e: Exception) {
        this.error("feed loader raised exception: ", e)

        val problemReport = if (e is HTTPHasProblemReportType) {
          e.problemReport
        } else {
          null
        }

        val message =
          this.services.borrowStrings.borrowBookFeedLoadingFailed(e.localizedMessage)

        this.steps.currentStepFailed(
          message = message,
          errorValue = FeedLoaderFailed(
            message = message,
            problemReport = problemReport,
            exception = e,
            attributesInitial = this.currentAttributesWith()
          ),
          exception = e
        )
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
                  message = this.services.borrowStrings.borrowBookBadBorrowFeed,
                  errorValue = FeedCorrupted(
                    exception = feedEntry.error,
                    attributes = this.currentAttributesWith()
                  ),
                  exception = feedEntry.error
                )
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
                  message = this.services.borrowStrings.borrowBookBadBorrowFeed,
                  errorValue = FeedCorrupted(
                    exception = feedEntry.error,
                    attributes = this.currentAttributesWith()
                  ),
                  exception = feedEntry.error
                )
                throw BookBorrowExceptionBadBorrowFeed(feedEntry.error)
              }
              is FeedEntryOPDS ->
                feedEntry
            }
          }
        }
      }

      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral -> {
        val message =
          this.services.borrowStrings.borrowBookFeedLoadingFailed(feedResult.message)

        this.steps.currentStepFailed(
          message = message,
          errorValue = FeedLoaderFailed(
            message = message,
            problemReport = feedResult.problemReport,
            exception = feedResult.exception,
            attributesInitial = this.currentAttributesWith(feedResult.attributes)
          ),
          exception = feedResult.exception
        )
        throw feedResult.exception
      }

      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication -> {
        val message =
          this.services.borrowStrings.borrowBookFeedLoadingFailed(feedResult.message)

        this.steps.currentStepFailed(
          message = message,
          errorValue = FeedLoaderFailed(
            message = message,
            problemReport = feedResult.problemReport,
            exception = feedResult.exception,
            attributesInitial = this.currentAttributesWith(feedResult.attributes)
          ),
          exception = feedResult.exception
        )
        throw feedResult.exception
      }
    }
  }

  /**
   * Check that a feed has at least one group.
   */

  private fun checkFeedHasGroups(
    uri: URI,
    groups: List<FeedGroup>
  ): List<FeedGroup> {

    val attribute =
      Pair("Feed URI", this.acquisition.uri.toASCIIString())

    if (groups.isEmpty()) {
      this.error("unexpectedly received feed with zero groups")
      val exception = BookBorrowExceptionBadBorrowFeed(IOException("No groups in feed: $uri"))
      this.steps.currentStepFailed(
        message = this.services.borrowStrings.borrowBookBadBorrowFeed,
        errorValue = FeedUnusable(
          message = this.services.borrowStrings.borrowBookBadBorrowFeed,
          attributes = this.currentAttributesWith(attribute)
        ),
        exception = exception
      )
      throw exception
    }
    return groups
  }

  /**
   * Check that a feed has at least one entry.
   */

  private fun checkFeedHasEntries(
    uri: URI,
    entries: List<FeedEntry>
  ): List<FeedEntry> {

    val attribute =
      Pair("Feed URI", this.acquisition.uri.toASCIIString())

    if (entries.isEmpty()) {
      this.error("unexpectedly received feed with no entries")
      val exception = BookBorrowExceptionBadBorrowFeed(IOException("No entries in feed: $uri"))
      this.steps.currentStepFailed(
        message = this.services.borrowStrings.borrowBookBadBorrowFeed,
        errorValue = FeedUnusable(
          message = this.services.borrowStrings.borrowBookBadBorrowFeed,
          attributes = this.currentAttributesWith(attribute)
        ),
        exception = exception
      )
      throw exception
    }
    return entries
  }

  /**
   * Complete borrowing given an OPDS feed entry.
   */

  private fun runAcquisitionBorrowForOPDSEntry(feedEntry: FeedEntryOPDS) {
    val availability = feedEntry.feedEntry.availability
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookBorrowForAvailability(availability))

    this.debug("received OPDS feed entry")
    this.debug("book availability is {}", availability)

    /*
     * Update the database.
     */

    this.debug("saving state to database")
    this.databaseEntry.writeOPDSEntry(feedEntry.feedEntry)
    this.debug("database state: {}", BookStatus.fromBook(this.databaseEntry.book))

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
            BookStatus.Held.HeldReady(
              id = this@BookBorrowTask.bookId,
              endDate = this@BookBorrowTask.someOrNull(a.endDate),
              isRevocable = a.revoke.isSome
            )
          )
          return false
        }

        /**
         * If the book is held, just notify the user of this fact by
         * setting the status.
         */

        override fun onHeld(a: OPDSAvailabilityHeld): Boolean {
          this@BookBorrowTask.debug("book is held, nothing more to do")
          this@BookBorrowTask.publishBookStatus(
            BookStatus.Held.HeldInQueue(
              this@BookBorrowTask.bookId,
              queuePosition = this@BookBorrowTask.someOrNull(a.position),
              startDate = this@BookBorrowTask.someOrNull(a.startDate),
              endDate = this@BookBorrowTask.someOrNull(a.endDate),
              isRevocable = a.revoke.isSome
            )
          )
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
          throw IllegalStateException("book is holdable, cannot continue!")
        }

        /**
         * If the book claims to be only "loanable", then something is
         * definitely wrong.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onLoanable(a: OPDSAvailabilityLoanable): Boolean {
          throw IllegalStateException("book is loanable, this is a server bug!")
        }

        /**
         * If the book is "loaned", then attempt to fulfill the book.
         */

        override fun onLoaned(a: OPDSAvailabilityLoaned): Boolean {
          this@BookBorrowTask.debug("book is loaned, fulfilling")
          this@BookBorrowTask.publishBookStatus(
            BookStatus.RequestingDownload(this@BookBorrowTask.bookId)
          )
          return true
        }

        /**
         * If the book is "open-access", then attempt to fulfill the
         * book.
         */

        override fun onOpenAccess(a: OPDSAvailabilityOpenAccess): Boolean {
          this@BookBorrowTask.debug("book is open access, fulfilling")
          this@BookBorrowTask.publishBookStatus(
            BookStatus.RequestingDownload(this@BookBorrowTask.bookId)
          )
          return true
        }

        /**
         * The server cannot return a "revoked" representation. Reaching
         * this code indicates a serious bug in the application.
         */

        override fun onRevoked(a: OPDSAvailabilityRevoked): Boolean {
          throw UnreachableCodeException()
        }
      })

    if (wantFulfill) {
      this.runAcquisitionFulfill(feedEntry.feedEntry)
    } else {
      this.steps.currentStepSucceeded("Borrow succeeded with availability $availability.")
    }
  }

  /**
   * Fulfill a book by hitting the generic or open access links.
   */

  private fun runAcquisitionFulfill(
    ee: OPDSAcquisitionFeedEntry
  ) {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfill)

    this.debug("fulfilling book")

    for (ea in ee.acquisitions) {
      when (ea.relation) {
        ACQUISITION_GENERIC,
        ACQUISITION_OPEN_ACCESS -> {
          val httpAuth = this.createHttpAuthIfRequired()
          this.runAcquisitionFulfillDoDownload(ea, httpAuth)
          this.runFetchCover(ee, httpAuth)
          return
        }
        ACQUISITION_BORROW,
        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {
        }
      }
    }

    val exception = BookBorrowExceptionNoUsableAcquisition()
    val message = this.services.borrowStrings.borrowBookFulfillNoUsableAcquisitions
    this.steps.currentStepFailed(
      message = message,
      errorValue = UnusableAcquisitions(message, this.currentAttributesWith()),
      exception = exception
    )
    throw exception
  }

  private fun runFetchCover(
    opdsEntry: OPDSAcquisitionFeedEntry,
    httpAuth: OptionType<HTTPAuthType>
  ) {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFetchingCover)
    this.debug("fetching cover")

    when (val result =
      BookCoverFetchTask(
        services = this.services,
        databaseEntry = this.databaseEntry,
        feedEntry = opdsEntry,
        type = Type.COVER,
        httpAuth = httpAuth
      ).call()) {
      is TaskResult.Success -> {
        this.debug("fetched cover successfully")
        this.steps.addAll(result.steps)
      }
      is TaskResult.Failure -> {
        this.debug("failed to fetch cover")
        this.steps.addAll(result.steps)
      }
    }

    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFetchingCover)
    this.debug("fetching thumbnail")

    when (val result =
      BookCoverFetchTask(
        services = this.services,
        databaseEntry = this.databaseEntry,
        feedEntry = opdsEntry,
        type = Type.THUMBNAIL,
        httpAuth = httpAuth
      ).call()) {
      is TaskResult.Success -> {
        this.debug("fetched thumbnail successfully")
        this.steps.addAll(result.steps)
      }
      is TaskResult.Failure -> {
        this.debug("failed to fetch thumbnail")
        this.steps.addAll(result.steps)
      }
    }
  }

  /**
   * The result of a download attempt.
   */

  private sealed class DownloadResult {

    /**
     * Downloading succeeded.
     */

    data class DownloadOK(
      val uri: URI,
      val file: File
    ) : DownloadResult()

    /**
     * Downloading failed.
     */

    data class DownloadFailed(
      val uri: URI,
      val status: Int,
      val problemReport: OptionType<HTTPProblemReport>,
      val exception: OptionType<Throwable>
    ) : DownloadResult()

    /**
     * Downloading was cancelled.
     */

    object DownloadCancelled : DownloadResult()
  }

  /**
   * A download listener that reports progress and sets the value of a future on completion
   * or errors.
   */

  private class DownloadListener(
    val downloadFuture: SettableFuture<DownloadResult>,
    val onDownloadProgress: (Long, Long, unconditional: Boolean) -> Unit
  ) : DownloadListenerType {

    override fun onDownloadStarted(
      download: DownloadType,
      expectedTotal: Long
    ) {
      this.onDownloadProgress.invoke(0L, expectedTotal, true)
    }

    override fun onDownloadDataReceived(
      download: DownloadType,
      runningTotal: Long,
      expectedTotal: Long
    ) {
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
      exception: OptionType<Throwable>
    ) {
      this.downloadFuture.set(
        DownloadFailed(
          uri = download.uri(),
          status = status,
          problemReport = problemReport,
          exception = exception
        )
      )
    }

    override fun onDownloadCompleted(
      download: DownloadType,
      file: File
    ) {
      this.downloadFuture.set(
        DownloadOK(
          uri = download.uri(),
          file = file
        )
      )
    }
  }

  private data class FileAndType(
    val file: File,
    val contentType: MIMEType
  )

  private fun runAcquisitionFulfillDoDownload(
    acquisition: OPDSAcquisition,
    httpAuth: OptionType<HTTPAuthType>
  ) {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillDownload)
    this.fulfillURI = acquisition.uri

    val (file, contentType) =
      if (this.acquisitionIsAudioBook(acquisition)) {
        this.runAcquisitionFulfillDoDownloadAudioBook(acquisition)
      } else {
        this.runAcquisitionFulfillDoDownloadWithDownloader(acquisition, httpAuth)
      }

    this.debug("download completed for {}", file)
    this.debug("content type is {}", contentType)

    this.steps.currentStepSucceeded(
      this.services.borrowStrings.borrowBookFulfillDownloaded(file, contentType)
    )

    return when (contentType) {
      this.contentTypeACSM ->
        this.runFulfillACSM(file)
      this.contentTypeSimplifiedBearerToken ->
        this.runFulfillSimplifiedBearerToken(acquisition, file)
      else ->
        this.saveFinalContent(
          file = file,
          expectedContentTypes = acquisition.availableFinalContentTypes(),
          receivedContentType = contentType
        )
    }
  }

  private fun runAcquisitionFulfillDoDownloadWithDownloader(
    acquisition: OPDSAcquisition,
    httpAuth: OptionType<HTTPAuthType>
  ): FileAndType {

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
          detailMessage = this.services.borrowStrings.borrowBookFulfillDownload,
          runningTotal = runningTotal,
          expectedTotal = expectedTotal,
          unconditional = unconditional
        )
      }

    val download =
      this.services.downloader.download(acquisition.uri, httpAuth, downloadListener)

    this.downloads[this.bookId] = download

    val result = try {
      downloadFuture.get(this.downloadTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
    } catch (ex: TimeoutException) {
      val message = this.services.borrowStrings.borrowBookFulfillTimedOut
      this.steps.currentStepFailed(
        message = message,
        errorValue = TimedOut(message, this.currentAttributesWith()),
        exception = ex
      )
      download.cancel()
      downloadFuture.cancel(true)
      throw IOException("Timed out", ex)
    } finally {
      this.downloads.remove(this.bookId)
    }

    return FileAndType(
      file = this.fileFromDownloadResult(result),
      contentType = MIMEParser.parseRaisingException(download.contentType)
    )
  }

  private fun runAcquisitionFulfillDoDownloadAudioBook(
    acquisition: OPDSAcquisition
  ): FileAndType {
    val targetContentType = acquisition.availableFinalContentTypes().first()
    this.debug("making audio book request for type {}", targetContentType.fullType)

    val audioBookCredentials: AudioBookCredentials? =
      this.account.loginState.credentials?.let { credentials ->
        when (credentials) {
          is AccountAuthenticationCredentials.Basic ->
            AudioBookCredentials.UsernamePassword(
              userName = credentials.userName.value,
              password = credentials.password.value
            )
          is AccountAuthenticationCredentials.OAuthWithIntermediary ->
            AudioBookCredentials.BearerToken(accessToken = credentials.accessToken)
        }
      }

    val strategy =
      this.services.audioBookManifestStrategies.createStrategy(
        AudioBookManifestRequest(
          targetURI = acquisition.uri,
          contentType = targetContentType,
          userAgent = PlayerUserAgent(HTTP.userAgent()),
          credentials = audioBookCredentials,
          services = this.services.services,
          cacheDirectory = this.cacheDirectory
        )
      )

    val subscription = strategy.events.subscribe { message ->
      this.downloadDataReceived(
        detailMessage = message,
        runningTotal = 50L,
        expectedTotal = 100L,
        unconditional = false
      )
    }

    return try {
      when (val result = strategy.execute()) {
        is TaskResult.Success -> {
          val outputFile =
            File.createTempFile("manifest", "data", this.cacheDirectory)
          outputFile.writeBytes(result.result.fulfilled.data)
          FileAndType(outputFile, result.result.fulfilled.contentType)
        }
        is TaskResult.Failure -> {
          val message = result.errors().first()
          val exception = IOException()
          this.steps.currentStepFailed(
            message = message,
            errorValue = TimedOut(message, this.currentAttributesWith()),
            exception = exception
          )
          throw exception
        }
      }
    } finally {
      subscription.unsubscribe()
    }
  }

  private fun acquisitionIsAudioBook(acquisition: OPDSAcquisition) =
    acquisition.availableFinalContentTypes().intersect(BookFormats.audioBookMimeTypes())
      .isNotEmpty()

  private fun fileFromDownloadResult(result: DownloadResult): File {
    return when (result) {
      is DownloadOK ->
        result.file

      is DownloadFailed -> {
        val exception =
          this.someOrNull(result.exception) ?: IOException("I/O error")
        val message =
          this.services.borrowStrings.borrowBookFulfillDownloadFailed(exception.localizedMessage)
        val uriAttribute =
          Pair("Book URI", result.uri.toASCIIString())

        this.steps.currentStepFailed(
          message = message,
          errorValue = HTTPRequestFailed(
            status = result.status,
            problemReport = this.someOrNull(result.problemReport),
            attributesInitial = this.currentAttributesWith(uriAttribute),
            message = message
          ),
          exception = exception
        )
        throw exception
      }

      DownloadCancelled -> {
        val exception = CancellationException()
        val message = this.services.borrowStrings.borrowBookFulfillCancelled
        this.steps.currentStepFailed(
          message = message,
          errorValue = BookStatusDownloadErrorDetails.DownloadCancelled(
            message,
            this.currentAttributesWith()
          ),
          exception = exception
        )
        throw exception
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
    expectedContentTypes: Set<MIMEType>,
    receivedContentType: MIMEType
  ) {
    this.steps.beginNewStep(
      this.services.borrowStrings.borrowBookSaving(receivedContentType, expectedContentTypes)
    )

    this.debug(
      "saving content {} (expected one of {}, received {})",
      file,
      expectedContentTypes,
      receivedContentType
    )

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
      formatHandle.setDRMKind(this.drmKind)

      when (val drmHandle = formatHandle.drmInformationHandle) {
        is BookDRMInformationHandle.ACSHandle -> {
          drmHandle.setAdobeRightsInformation(this.adobeLoan)
          Unit
        }
        is BookDRMInformationHandle.LCPHandle,
        is BookDRMInformationHandle.NoneHandle -> {
          // Nothing required
        }
      }

      when (formatHandle) {
        is BookDatabaseEntryFormatHandleEPUB -> {
          formatHandle.copyInBook(file)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandlePDF -> {
          formatHandle.copyInBook(file)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandleAudioBook -> {
          formatHandle.copyInManifestAndURI(file.readBytes(), this.fulfillURI)
          updateStatus()
        }
      }
    } else {
      this.error("database entry does not have a format handle for {}", handleContentType)
      val exception = BookUnsupportedTypeException(handleContentType)
      val message = this.services.borrowStrings.borrowBookSavingCheckingContentTypeUnacceptable
      this.steps.currentStepFailed(
        message = message,
        errorValue = UnsupportedType(message, this.currentAttributesWith()),
        exception = exception
      )
      throw exception
    }
  }

  /*
   * If we expect a specific content type, but the server actually delivers application/octet-stream,
   * then assume that the server delivered the expected type. Otherwise, check that the received
   * type matches the expected type.
   */

  private fun checkExpectedContentType(
    expectedContentTypes: Set<MIMEType>,
    receivedContentType: MIMEType
  ): MIMEType {

    this.steps.beginNewStep(
      this.services.borrowStrings.borrowBookSavingCheckingContentType(
        receivedContentType, expectedContentTypes
      )
    )

    Preconditions.checkArgument(
      !expectedContentTypes.isEmpty(),
      "At least one expected content type"
    )

    /*
     * Attempt to find our received type in our set of expected types.
     */

    for (expectedContentType in expectedContentTypes) {
      if (expectedContentType.fullType == receivedContentType.fullType) {
        return receivedContentType
      }
    }

    /*
     * Handle the 'application/json' type as an exception.
     *
     * SIMPLY-2928: Overdrive may return 'application/json' instead of
     * the expected 'application/vnd.overdrive.circulation.api+json' type.
     */

    val isAudiobook = expectedContentTypes
      .intersect(BookFormats.audioBookMimeTypes())
      .isNotEmpty()

    if (isAudiobook) {
      when (receivedContentType.fullType) {
        this.contentTypeJson.fullType -> {
          this.debug(
            "expected one of {} but received {} (acceptable)",
            expectedContentTypes,
            receivedContentType
          )

          this.steps.currentStepSucceeded(this.services.borrowStrings.borrowBookSavingCheckingContentTypeOK)
          return expectedContentTypes.first()
        }
      }
    }

    /*
     * Handle the 'application/octet-stream' type as an exception.
     */

    if (receivedContentType == this.contentTypeOctetStream) {
      this.debug(
        "expected one of {} but received {} (acceptable)",
        expectedContentTypes,
        receivedContentType
      )

      this.steps.currentStepSucceeded(this.services.borrowStrings.borrowBookSavingCheckingContentTypeOK)
      return expectedContentTypes.first()
    }

    /*
     * No match found!
     */

    this.debug(
      "expected {} but received {} (unacceptable)",
      expectedContentTypes,
      receivedContentType
    )

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
        received = receivedContentType
      )

    val message = this.services.borrowStrings.borrowBookSavingCheckingContentTypeUnacceptable
    this.steps.currentStepFailed(
      message = message,
      errorValue = UnsupportedType(
        message = message,
        attributes = this.currentAttributesWith(Pair("Content Type", receivedContentType.fullType))
      ),
      exception = exception
    )

    throw exception
  }

  /**
   * Fulfill the given ACSM file, if Adobe DRM is supported. Otherwise, fail.
   */

  private fun runFulfillACSM(
    file: File
  ) {
    this.debug("fulfilling ACSM file")

    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillACSM)

    /*
     * The ACSM file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. The Adobe library will then take up
     * to roughly ten seconds to start fulfilling the ACSM. This call
     * effectively sets the download progress bar to 0% so that it doesn't look
     * as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(
      detailMessage = this.services.borrowStrings.borrowBookFulfillACSM,
      runningTotal = 0L,
      expectedTotal = 100L,
      unconditional = true
    )

    this.drmKind = BookDRMKind.ACS
    val adept = this.services.adobeDRM
    return if (adept != null) {
      this.debug("DRM support is available, using DRM connector")
      this.runFulfillACSMWithConnector(adept, file)
    } else {
      this.debug("DRM support is unavailable, cannot continue!")
      val ex = DRMUnsupportedException("DRM support is not available")
      this.steps.currentStepFailed(
        message = this.services.borrowStrings.borrowBookFulfillDRMNotSupported,
        errorValue = DRMUnsupportedSystem(
          message = this.services.borrowStrings.borrowBookFulfillDRMNotSupported,
          system = this.adobeACS
        ),
        exception = ex
      )
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
        receivedContentType = this.contentTypeEPUB
      )
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
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillACSMConnector)

    this.downloadDataReceived(
      detailMessage = this.services.borrowStrings.borrowBookFulfillACSMConnector,
      runningTotal = 0,
      expectedTotal = 100,
      unconditional = true
    )

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
            detailMessage = this.services.borrowStrings.borrowBookFulfillACSMConnector,
            runningTotal = progress.toLong(),
            expectedTotal = 100,
            unconditional = false
          )
        },
        outputFile,
        acsmBytes,
        credentials.userID
      )

    val fulfillment =
      try {
        val seconds = this.downloadTimeoutDuration.standardSeconds
        this.logger.debug("waiting for fulfillment for {} seconds", seconds)
        future.get(seconds, TimeUnit.SECONDS)
      } catch (e: TimeoutException) {
        val message = this.services.borrowStrings.borrowBookFulfillTimedOut
        this.steps.currentStepFailed(
          message = message,
          errorValue = TimedOut(message, this.currentAttributesWith()),
          exception = e
        )
        this.downloads[this.bookId]?.cancel()
        throw IOException("Timed out", e)
      } catch (e: ExecutionException) {
        throw when (val cause = e.cause!!) {
          is CancellationException -> {
            val message = this.services.borrowStrings.borrowBookFulfillCancelled
            this.steps.currentStepFailed(
              message = message,
              errorValue = BookStatusDownloadErrorDetails.DownloadCancelled(
                message,
                this.currentAttributesWith()
              ),
              exception = cause
            )
            cause
          }
          is AdobeDRMFulfillmentException -> {
            val message =
              this.services.borrowStrings.borrowBookFulfillACSMConnectorFailed(cause.errorCode)
            this.steps.currentStepFailed(
              message = message,
              errorValue = DRMFailure(
                system = this.adobeACS,
                errorCode = cause.errorCode,
                message = message
              ),
              exception = cause
            )
            cause
          }
          else -> {
            this.steps.currentStepFailed(
              message = this.services.borrowStrings.borrowBookFulfillACSMFailed,
              errorValue = UnexpectedException(cause, this.currentAttributesWith()),
              exception = cause
            )
            cause
          }
        }
      } catch (e: Throwable) {
        this.steps.currentStepFailed(
          message = this.services.borrowStrings.borrowBookFulfillACSMFailed,
          errorValue = UnexpectedException(e, this.currentAttributesWith()),
          exception = e
        )
        throw e
      }

    this.steps.currentStepSucceeded(this.services.borrowStrings.borrowBookFulfillACSMConnectorOK)
    return fulfillment
  }

  /**
   * Retrieve the post-activation device credentials. These can only exist if the device
   * has been activated.
   */

  private fun runFulfillACSMWithConnectorGetCredentials(): AccountAuthenticationAdobePostActivationCredentials {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillACSMGettingDeviceCredentials)

    val credentials =
      this.getRequiredAccountCredentials().adobeCredentials?.postActivationCredentials

    if (credentials == null) {
      val exception = BookBorrowExceptionDeviceNotActivated()
      val message =
        this.services.borrowStrings.borrowBookFulfillACSMGettingDeviceCredentialsNotActivated
      this.steps.currentStepFailed(
        message,
        errorValue = DRMDeviceNotActive(
          system = this.adobeACS,
          message = message
        ),
        exception = exception
      )
      throw exception
    }

    this.steps.currentStepSucceeded(
      this.services.borrowStrings.borrowBookFulfillACSMGettingDeviceCredentialsOK
    )
    return credentials
  }

  /**
   * Read the ACSM file.
   */

  private fun runFulfillACSMWithConnectorReadACSM(file: File): ByteArray {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillACSMRead)

    try {
      return FileUtilities.fileReadBytes(file)
    } catch (e: Exception) {
      val message = this.services.borrowStrings.borrowBookFulfillACSMReadFailed
      this.steps.currentStepFailed(
        message = message,
        errorValue = DRMUnreadableACSM(
          system = this.adobeACS,
          message = message
        ),
        exception = e
      )
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
      override fun uri(): URI {
        return URI.create("urn:unavailable")
      }

      override fun cancel() {
        val net = connector.netProvider
        net.cancel()
      }

      override fun getContentType(): String {
        return this@BookBorrowTask.contentTypeOctetStream.fullType
      }
    }
  }

  /**
   * Check that we actually support DRMd content of the given type.
   */

  private fun runFulfillACSMWithConnectorCheckContentType(parsed: AdobeAdeptFulfillmentToken) {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillACSMCheckContentType)

    val contentType = MIMEParser.parseRaisingException(parsed.format)
    if (this.contentTypeEPUB.fullType != contentType.fullType) {
      val exception = BookUnsupportedTypeException(contentType)
      val message = this.services.borrowStrings.borrowBookFulfillACSMUnsupportedContentType
      this.steps.currentStepFailed(
        message = message,
        errorValue = DRMUnsupportedContentType(
          system = this.adobeACS,
          contentType = contentType,
          message = message
        ),
        exception = exception
      )
      throw exception
    }

    this.steps.currentStepSucceeded(
      this.services.borrowStrings.borrowBookFulfillACSMCheckContentTypeOK(contentType)
    )
  }

  /**
   * Parse a series of bytes that are expected to comprise an ACSM file.
   */

  private fun runFulfillACSMWithConnectorParse(acsmBytes: ByteArray): AdobeAdeptFulfillmentToken {
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillACSMParse)

    return try {
      AdobeAdeptFulfillmentToken.parseFromBytes(acsmBytes)
    } catch (e: Exception) {
      val message = this.services.borrowStrings.borrowBookFulfillACSMParseFailed
      this.steps.currentStepFailed(
        message = message,
        errorValue = DRMUnparseableACSM(
          system = this.adobeACS,
          message = message
        ),
        exception = e
      )
      throw e
    }
  }

  private fun runFulfillSimplifiedBearerToken(
    acquisition: OPDSAcquisition,
    file: File
  ) {
    this.debug("fulfilling Simplified bearer token file")

    this.steps.beginNewStep(this.services.borrowStrings.borrowBookFulfillBearerToken)

    /*
     * The bearer token file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. This call effectively sets the download progress bar
     * to 0% so that it doesn't look as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(
      detailMessage = this.services.borrowStrings.borrowBookFulfillBearerToken,
      runningTotal = 0L,
      expectedTotal = 100L,
      unconditional = true
    )

    val token = try {
      SimplifiedBearerTokenJSON.deserializeFromFile(ObjectMapper(), LocalDateTime.now(), file)
    } catch (ex: Exception) {
      this.error("failed to parse bearer token: {}: ", acquisition.uri, ex)
      val message = this.services.borrowStrings.borrowBookFulfillUnparseableBearerToken
      this.steps.currentStepFailed(
        message = message,
        errorValue = UnparseableBearerToken(message, this.currentAttributesWith()),
        exception = ex
      )
      throw ex
    }

    this.steps.currentStepSucceeded(this.services.borrowStrings.borrowBookFulfillBearerTokenOK)

    val nextAcquisition =
      OPDSAcquisition(
        ACQUISITION_GENERIC,
        token.location,
        acquisition.type,
        acquisition.indirectAcquisitions
      )

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
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookBundledCopy)

    this.publishBookStatus(
      BookStatus.RequestingLoan(
        id = this.bookId,
        detailMessage = this.services.borrowStrings.borrowBookBundledCopy
      )
    )

    this.fulfillURI = this.acquisition.uri
    val file = this.databaseEntry.temporaryFile()
    val buffer = ByteArray(8192)

    try {
      return FileOutputStream(file).use { output ->
        this.services.bundledContent.resolve(this.acquisition.uri).use { stream ->
          val size = stream.available().toLong()
          var consumed = 0L
          this.downloadDataReceived(
            detailMessage = this.services.borrowStrings.borrowBookBundledCopy,
            runningTotal = consumed,
            expectedTotal = size,
            unconditional = true
          )

          while (true) {
            val r = stream.read(buffer)
            if (r == -1) {
              break
            }
            consumed += r.toLong()
            output.write(buffer, 0, r)

            this.downloadDataReceived(
              detailMessage = this.services.borrowStrings.borrowBookBundledCopy,
              runningTotal = consumed,
              expectedTotal = size,
              unconditional = false
            )
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
          receivedContentType = this.contentTypeOctetStream
        )

        this.publishBookStatus(BookStatus.fromBook(this.databaseEntry.book))
      }
    } catch (e: Exception) {
      this.logger.error("could not copy content: ", e)

      val message = this.services.borrowStrings.borrowBookBundledCopyFailed
      this.steps.currentStepFailed(
        message = message,
        errorValue = BundledCopyFailed(message, this.currentAttributesWith()),
        exception = e
      )
      FileUtilities.fileDelete(file)
      throw e
    }
  }

  private fun runAcquisitionContentURI() {
    this.debug("acquisition is content")
    this.steps.beginNewStep(this.services.borrowStrings.borrowBookContentCopy)

    this.publishBookStatus(
      BookStatus.RequestingLoan(
        id = this.bookId,
        detailMessage = this.services.borrowStrings.borrowBookContentCopy
      )
    )

    this.fulfillURI = this.acquisition.uri
    val file = this.databaseEntry.temporaryFile()
    val buffer = ByteArray(8192)

    try {
      return FileOutputStream(file).use { output ->
        val uriText = this.acquisition.uri.toString()

        val streamMaybe =
          this.services.contentResolver.openInputStream(Uri.parse(uriText))
            ?: throw FileNotFoundException(uriText)

        streamMaybe.use { stream ->
          val size = stream.available().toLong()
          var consumed = 0L
          this.downloadDataReceived(
            detailMessage = this.services.borrowStrings.borrowBookContentCopy,
            runningTotal = consumed,
            expectedTotal = size,
            unconditional = true
          )

          while (true) {
            val r = stream.read(buffer)
            if (r == -1) {
              break
            }
            consumed += r.toLong()
            output.write(buffer, 0, r)

            this.downloadDataReceived(
              detailMessage = this.services.borrowStrings.borrowBookContentCopy,
              runningTotal = consumed,
              expectedTotal = size,
              unconditional = false
            )
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
          receivedContentType = this.contentTypeOctetStream
        )

        this.publishBookStatus(BookStatus.fromBook(this.databaseEntry.book))
      }
    } catch (e: Exception) {
      this.logger.error("could not copy content: ", e)

      val message = this.services.borrowStrings.borrowBookContentCopyFailed
      this.steps.currentStepFailed(
        message = message,
        errorValue = ContentCopyFailed(message, this.currentAttributesWith()),
        exception = e
      )
      FileUtilities.fileDelete(file)
      throw e
    }
  }

  private fun publishBookStatus(status: BookStatus) {
    val book =
      if (this.databaseEntryInitialized) {
        this.databaseEntry.book
      } else {
        this.warn("publishing status using a fake book")
        this.bookInitial
      }
    this.services.bookRegistry.update(BookWithStatus(book, status))
  }

  private fun downloadDataReceived(
    detailMessage: String,
    runningTotal: Long,
    expectedTotal: Long,
    unconditional: Boolean
  ) {

    /*
     * Because "data received" updates happen at such a huge rate, we want
     * to ensure that updates to the book status are rate limited to avoid
     * overwhelming the UI. Book updates are limited to a rate of ten per
     * second.
     */

    val timeNow = this.services.clock.clockNow()
    val period = Period(this.downloadTimeThen, timeNow, PeriodType.millis())
    if (period.millis >= 100 || unconditional) {
      val status =
        BookStatus.Downloading(
          id = this.bookId,
          detailMessage = detailMessage,
          currentTotalBytes = runningTotal,
          expectedTotalBytes = expectedTotal
        )
      this.services.bookRegistry.update(BookWithStatus(this.databaseEntry.book, status))
      this.downloadRunningTotal = runningTotal
      this.downloadTimeThen = timeNow
    }
  }
}
