package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.DateTime
import org.joda.time.Duration
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptLoan
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
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke
import org.nypl.simplified.books.book_registry.BookStatusRevokeErrorDetails
import org.nypl.simplified.books.book_registry.BookStatusRevokeErrorDetails.*
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed
import org.nypl.simplified.books.book_registry.BookStatusRevokeResult
import org.nypl.simplified.books.book_registry.BookStatusRevoked
import org.nypl.simplified.books.book_registry.BookStatusType
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BookRevokeExceptionBadFeed
import org.nypl.simplified.books.controller.api.BookRevokeExceptionDeviceNotActivated
import org.nypl.simplified.books.controller.api.BookRevokeExceptionNoCredentials
import org.nypl.simplified.books.controller.api.BookRevokeExceptionNotRevocable
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class BookRevokeTask(
  private val account: AccountType,
  private val adobeDRM: AdobeAdeptExecutorType?,
  private val bookID: BookID,
  private val bookRegistry: BookRegistryType,
  private val feedLoader: FeedLoaderType,
  private val revokeStrings: BookRevokeStringResourcesType,
  private val revokeACSTimeoutDuration: Duration = Duration.standardMinutes(1L),
  private val revokeServerTimeoutDuration: Duration = Duration.standardMinutes(3L))
  : Callable<BookStatusRevokeResult> {

  private val adobeACS = "Adobe ACS"

  private lateinit var databaseEntry: BookDatabaseEntryType
  private var databaseEntryInitialized: Boolean = false

  private val logger = LoggerFactory.getLogger(BookRevokeTask::class.java)
  private val steps = TaskRecorder.create<BookStatusRevokeErrorDetails>()

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}] ${message}", this.bookID.brief(), *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}] ${message}", this.bookID.brief(), *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}] ${message}", this.bookID.brief(), *arguments)

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

  private fun publishBookStatus(status: BookStatusType) {
    val book =
      if (this.databaseEntryInitialized) {
        this.databaseEntry.book
      } else {
        this.warn("publishing book status with fake book!")

        /**
         * Note that this is a synthesized value because we need to be able to open the book
         * database to get a real book value, and that database call might fail. If the call fails,
         * we have no "book" that we can refer to in order to publish a "book revoke has failed"
         * status for the book, so we use this fake book in that (rare) situation.
         */

        val entry =
          OPDSAcquisitionFeedEntry.newBuilder(
            this.bookID.value(),
            "",
            DateTime.now(),
            OPDSAvailabilityLoanable.get())
            .build()

        Book(
          this.bookID,
          this.account.id(),
          null,
          null,
          entry,
          listOf())
      }

    this.bookRegistry.update(BookWithStatus.create(book, status))
  }

  private fun publishRequestingRevokeStatus() {
    this.publishBookStatus(BookStatusRequestingRevoke(this.bookID, this.steps.currentStep()!!.description))
  }

  private fun publishRevokedStatus() {
    this.publishBookStatus(BookStatusRevoked(this.bookID))

  }

  override fun call(): BookStatusRevokeResult {
    return try {
      this.steps.beginNewStep(this.revokeStrings.revokeStarted)
      this.debug("revoke")

      this.setupBookDatabaseEntry()
      this.revokeFormatHandle()
      this.revokeNotifyServer()
      this.revokeNotifyServerDeleteBook()
      this.bookRegistry.clearFor(this.bookID)

      BookStatusRevokeResult(this.steps.finish())
    } catch (e: Throwable) {
      this.error("revoke failed: ", e)

      val step = this.steps.currentStep()!!
      if (step.exception == null) {
        this.steps.currentStepFailed(
          message = this.pickUsableMessage(step.resolution, e),
          errorValue = step.errorValue,
          exception = e)
      }

      val result = BookStatusRevokeResult(this.steps.finish())
      this.publishBookStatus(BookStatusRevokeFailed(this.bookID, result))
      result
    } finally {
      this.debug("finished")
    }
  }

  private fun revokeNotifyServer() {
    this.debug("notifying server of revocation")
    this.steps.beginNewStep(this.revokeStrings.revokeServerNotify)
    this.publishRequestingRevokeStatus()

    val availability = this.databaseEntry.book.entry.availability
    this.debug("availability is {}", availability)

    return when (availability) {
      is OPDSAvailabilityHeldReady -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.HOLD)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.steps.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
        }
      }

      is OPDSAvailabilityHeld -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.HOLD)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.steps.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
        }
      }

      is OPDSAvailabilityHoldable -> {
        val exception = BookRevokeExceptionNotRevocable()
        this.steps.currentStepFailed(
          this.revokeStrings.revokeServerNotifyNotRevocable(availability.javaClass.simpleName),
          NotRevocable,
          exception)
        throw exception
      }

      is OPDSAvailabilityLoaned -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.LOAN)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.steps.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
        }
      }

      is OPDSAvailabilityLoanable -> {
        val exception = BookRevokeExceptionNotRevocable()
        this.steps.currentStepFailed(
          this.revokeStrings.revokeServerNotifyNotRevocable(availability.javaClass.simpleName),
          NotRevocable,
          exception)
        throw exception
      }

      is OPDSAvailabilityOpenAccess -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.LOAN)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.steps.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
        }
      }

      is OPDSAvailabilityRevoked ->
        this.revokeNotifyServerURI(availability.revoke, RevokeType.LOAN)

      else ->
        throw UnreachableCodeException()
    }
  }

  private fun revokeNotifyServerURI(
    targetURI: URI,
    revokeType: RevokeType) {
    this.debug("notifying server of {} revocation via {}", revokeType, targetURI)
    this.steps.beginNewStep(this.revokeStrings.revokeServerNotifyURI(targetURI))
    this.publishRequestingRevokeStatus()

    val feed =
      this.revokeNotifyServerURIFeed(targetURI)
    val entry =
      this.revokeNotifyServerURIProcessFeed(feed)

    this.revokeNotifyServerSaveNewEntry(entry)
  }

  private fun revokeNotifyServerSaveNewEntry(entry: FeedEntryOPDS) {
    this.debug("saving received OPDS entry")
    this.steps.beginNewStep(this.revokeStrings.revokeServerNotifySavingEntry)
    this.publishRequestingRevokeStatus()

    try {
      this.databaseEntry.writeOPDSEntry(entry.feedEntry)
    } catch (e: Exception) {
      this.steps.currentStepFailed(
        this.revokeStrings.revokeServerNotifySavingEntryFailed, null, e)
      throw e
    }
  }

  private fun revokeNotifyServerDeleteBook() {
    this.debug("deleting book")
    this.steps.beginNewStep(this.revokeStrings.revokeDeleteBook)
    this.publishRevokedStatus()

    try {
      this.databaseEntry.delete()
    } catch (e: Throwable) {
      this.steps.currentStepFailed(this.revokeStrings.revokeDeleteBookFailed, null, e)
      throw e
    }
  }

  private fun revokeNotifyServerURIFeed(targetURI: URI): Feed {
    val httpAuth = this.createHttpAuthIfRequired()

    /*
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    val feedResult = try {
      this.feedLoader.fetchURIRefreshing(targetURI, httpAuth, "PUT")
        .get(this.revokeServerTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
      this.steps.currentStepFailed(
        this.revokeStrings.revokeServerNotifyFeedTimedOut, null, e)
      throw e
    } catch (e: ExecutionException) {
      val ex = e.cause!!
      this.steps.currentStepFailed(
        this.revokeStrings.revokeServerNotifyFeedTimedOut,
        FeedLoaderFailed(null, ex),
        ex)
      throw ex
    }

    return when (feedResult) {
      is FeedLoaderSuccess -> {
        this.steps.currentStepSucceeded(this.revokeStrings.revokeServerNotifyFeedOK)
        feedResult.feed
      }

      is FeedLoaderFailedGeneral -> {
        this.steps.currentStepFailed(
          this.revokeStrings.revokeServerNotifyFeedFailed,
          FeedLoaderFailed(feedResult.problemReport, feedResult.exception))
        throw feedResult.exception
      }

      is FeedLoaderFailedAuthentication -> {
        this.steps.currentStepFailed(
          this.revokeStrings.revokeServerNotifyFeedFailed,
          FeedLoaderFailed(feedResult.problemReport, feedResult.exception))
        throw feedResult.exception
      }
    }
  }

  private fun revokeNotifyServerURIProcessFeed(feed: Feed): FeedEntryOPDS {
    this.debug("processing server revocation feed")
    this.steps.beginNewStep(this.revokeStrings.revokeServerNotifyProcessingFeed)
    this.publishRequestingRevokeStatus()

    if (feed.size == 0) {
      val exception = BookRevokeExceptionBadFeed()
      this.steps.currentStepFailed(this.revokeStrings.revokeServerNotifyFeedEmpty, FeedUnusable, exception)
      throw exception
    }

    return when (feed) {
      is Feed.FeedWithoutGroups -> {
        when (val feedEntry = feed.entriesInOrder[0]) {
          is FeedEntryCorrupt -> {
            val exception = BookRevokeExceptionBadFeed()
            this.steps.currentStepFailed(
              this.revokeStrings.revokeServerNotifyFeedCorrupt,
              FeedCorrupted(feedEntry.error),
              exception)
            throw exception
          }
          is FeedEntryOPDS ->
            feedEntry
        }
      }
      is Feed.FeedWithGroups -> {
        val exception = BookRevokeExceptionBadFeed()
        this.steps.currentStepFailed(
          this.revokeStrings.revokeServerNotifyFeedWithGroups,
          FeedUnusable,
          exception)
        throw exception
      }
    }
  }

  private fun revokeFormatHandle() {
    this.debug("revoking via format handle")
    this.steps.beginNewStep(this.revokeStrings.revokeFormat)
    this.publishRequestingRevokeStatus()

    return when (val handle = this.databaseEntry.findPreferredFormatHandle()) {
      is BookDatabaseEntryFormatHandleEPUB ->
        this.revokeFormatHandleEPUB(handle)
      is BookDatabaseEntryFormatHandlePDF ->
        this.revokeFormatHandlePDF(handle)
      is BookDatabaseEntryFormatHandleAudioBook ->
        this.revokeFormatHandleAudioBook(handle)
      null -> {
        this.debug("no format handle available, nothing to do!")
        this.steps.currentStepSucceeded(this.revokeStrings.revokeFormatNothingToDo)
      }
    }
  }

  private fun revokeFormatHandleEPUB(handle: BookDatabaseEntryFormatHandleEPUB) {
    this.debug("revoking via EPUB format handle")
    this.steps.beginNewStep(this.revokeStrings.revokeFormatSpecific("EPUB"))
    this.publishRequestingRevokeStatus()

    val adobeRights = handle.format.adobeRights
    if (adobeRights != null) {
      this.revokeFormatHandleEPUBAdobe(handle, adobeRights)
    } else {
      this.debug("no Adobe rights, nothing to do!")
    }
  }

  private fun revokeFormatHandleEPUBAdobe(
    handle: BookDatabaseEntryFormatHandleEPUB,
    adobeRights: AdobeAdeptLoan) {
    this.debug("revoking Adobe ACS loan")
    this.steps.beginNewStep(this.revokeStrings.revokeACSLoan)
    this.publishRequestingRevokeStatus()

    /*
     * If the loan is not returnable, then there's no point trying to return it!
     */

    if (!adobeRights.isReturnable) {
      this.debug("loan is not returnable")
      this.steps.currentStepSucceeded(this.revokeStrings.revokeACSLoanNotReturnable)
      this.deleteAdobeRights(handle)
      return
    }

    /*
     * If the Adept executor is not provided, it means that this build of the application
     * has no support for Adobe DRM. We don't treat a missing Adept executor as failure case
     * because if support for Adobe DRM is dropped in the future, it would suddenly become
     * impossible for users to revoke loans that were previously made with activated devices.
     */

    if (this.adobeDRM == null) {
      this.debug("DRM is not supported")
      this.steps.currentStepSucceeded(this.revokeStrings.revokeACSLoanNotSupported)
      this.deleteAdobeRights(handle)
      return
    }

    this.revokeFormatHandleEPUBAdobeExecute(this.adobeDRM, adobeRights)
    this.deleteAdobeRights(handle)
  }

  /**
   * Execute the DRM connector commands required to revoke a loan.
   */

  private fun revokeFormatHandleEPUBAdobeExecute(
    adobeDRM: AdobeAdeptExecutorType,
    adobeRights: AdobeAdeptLoan) {

    val credentials =
      this.revokeFormatHandleEPUBAdobeWithConnectorGetCredentials()

    this.steps.beginNewStep(this.revokeStrings.revokeACSExecute)
    this.publishRequestingRevokeStatus()

    val adeptFuture =
      AdobeDRMExtensions.revoke(adobeDRM, adobeRights, credentials.userID)

    try {
      adeptFuture.get(this.revokeACSTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
    } catch (e : TimeoutException) {
      this.steps.currentStepFailed(
        message = this.revokeStrings.revokeACSTimedOut,
        errorValue = null,
        exception = e)
      throw e
    } catch (e: ExecutionException) {
      throw when (val cause = e.cause!!) {
        is CancellationException -> {
          this.steps.currentStepFailed(
            message = this.revokeStrings.revokeBookCancelled,
            errorValue = null,
            exception = cause)
          cause
        }
        is AdobeDRMExtensions.AdobeDRMRevokeException -> {
          this.steps.currentStepFailed(
            message = this.revokeStrings.revokeBookACSConnectorFailed(cause.errorCode),
            errorValue = DRMError.DRMFailure(this.adobeACS, cause.errorCode),
            exception = cause)
          cause
        }
        else -> {
          this.steps.currentStepFailed(
            message = this.revokeStrings.revokeBookACSFailed,
            errorValue = null,
            exception = cause)
          cause
        }
      }
    } catch (e: Throwable) {
      this.steps.currentStepFailed(
        message = this.revokeStrings.revokeBookACSFailed,
        errorValue = null,
        exception = e)
      throw e
    }

    this.steps.currentStepSucceeded(this.revokeStrings.revokeACSExecuteOK)
  }

  /**
   * Retrieve the post-activation device credentials. These can only exist if the device
   * has been activated.
   */

  private fun revokeFormatHandleEPUBAdobeWithConnectorGetCredentials(): AccountAuthenticationAdobePostActivationCredentials {
    this.debug("getting Adobe ACS credentials")
    this.steps.beginNewStep(this.revokeStrings.revokeACSGettingDeviceCredentials)
    this.publishRequestingRevokeStatus()

    val credentials =
      this.someOrNull(this.getRequiredAccountCredentials().adobePostActivationCredentials())

    if (credentials == null) {
      val exception = BookRevokeExceptionDeviceNotActivated()
      this.steps.currentStepFailed(
        this.revokeStrings.revokeACSGettingDeviceCredentialsNotActivated,
        errorValue = DRMError.DRMDeviceNotActive(this.adobeACS),
        exception = exception)
      throw exception
    }

    this.steps.currentStepSucceeded(this.revokeStrings.revokeACSGettingDeviceCredentialsOK)
    return credentials
  }

  private fun <T> someOrNull(option: OptionType<T>): T? {
    return if (option is Some<T>) {
      option.get()
    } else {
      null
    }
  }

  private fun deleteAdobeRights(handle: BookDatabaseEntryFormatHandleEPUB) {
    this.debug("deleting Adobe ACS loan")
    this.steps.beginNewStep(this.revokeStrings.revokeACSDeleteRights)
    this.publishRequestingRevokeStatus()

    try {
      handle.setAdobeRightsInformation(null)
    } catch (e: Exception) {
      this.steps.currentStepFailed(this.revokeStrings.revokeACSDeleteRightsFailed, null, e)
      throw e
    }
  }

  private fun revokeFormatHandleAudioBook(handle: BookDatabaseEntryFormatHandleAudioBook) {
    this.debug("revoking via AudioBook format handle")
    this.steps.beginNewStep(this.revokeStrings.revokeFormatSpecific("AudioBook"))
    this.publishRequestingRevokeStatus()

    this.steps.currentStepSucceeded(this.revokeStrings.revokeFormatNothingToDo)
  }

  private fun revokeFormatHandlePDF(handle: BookDatabaseEntryFormatHandlePDF) {
    this.debug("revoking via PDF format handle")
    this.steps.beginNewStep(this.revokeStrings.revokeFormatSpecific("PDF"))
    this.publishRequestingRevokeStatus()

    this.steps.currentStepSucceeded(this.revokeStrings.revokeFormatNothingToDo)
  }

  private fun setupBookDatabaseEntry() {
    this.steps.beginNewStep(this.revokeStrings.revokeBookDatabaseLookup)

    try {
      this.debug("setting up book database entry")
      val database = this.account.bookDatabase()
      this.databaseEntry = database.entry(this.bookID)
      this.databaseEntryInitialized = true
      this.publishRequestingRevokeStatus()
      this.steps.currentStepSucceeded(this.revokeStrings.revokeBookDatabaseLookupOK)
    } catch (e: Exception) {
      this.error("failed to set up book database entry: ", e)
      this.steps.currentStepFailed(this.revokeStrings.revokeBookDatabaseLookupFailed, null, e)
      throw e
    }
  }

  /**
   * If the account requires credentials, create HTTP auth details. If no credentials
   * are provided, throw an exception.
   */

  private fun createHttpAuthIfRequired(): OptionType<HTTPAuthType> {
    return if (this.account.requiresCredentials()) {
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
    val loginState = this.account.loginState()
    val credentials = loginState.credentials
    if (credentials != null) {
      return credentials
    } else {
      this.error("revocation requires credentials, but none are available")
      val exception = BookRevokeExceptionNoCredentials()
      this.steps.currentStepFailed(
        this.revokeStrings.revokeCredentialsRequired,
        NoCredentialsAvailable,
        exception)
      throw exception
    }
  }

  private enum class RevokeType {
    LOAN, HOLD
  }
}
