package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.DateTime
import org.joda.time.Duration
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BookRevokeExceptionBadFeed
import org.nypl.simplified.books.controller.api.BookRevokeExceptionDeviceNotActivated
import org.nypl.simplified.books.controller.api.BookRevokeExceptionNotRevocable
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.feeds.api.FeedHTTPTransportException
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSAvailabilityType
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class BookRevokeTask(
  private val accountID: AccountID,
  profileID: ProfileID,
  profiles: ProfilesDatabaseType,
  private val adobeDRM: AdobeAdeptExecutorType?,
  private val bookID: BookID,
  private val bookRegistry: BookRegistryType,
  private val feedLoader: FeedLoaderType,
  private val revokeStrings: BookRevokeStringResourcesType,
  private val revokeACSTimeoutDuration: Duration = Duration.standardMinutes(1L),
  private val revokeServerTimeoutDuration: Duration = Duration.standardMinutes(3L)
) : AbstractBookTask(accountID, profileID, profiles) {

  private lateinit var databaseEntry: BookDatabaseEntryType
  private val adobeACS = "Adobe ACS"

  override val logger: Logger =
    LoggerFactory.getLogger(BookRevokeTask::class.java)

  override val taskRecorder: TaskRecorderType =
    TaskRecorder.create()

  override fun execute(account: AccountType): TaskResult.Success<Unit> {
    this.debug("revoke")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeStarted)
    this.publishRequestingRevokeStatus()
    this.setupBookDatabaseEntry(account)
    this.revokeFormatHandle(account)
    this.revokeNotifyServer(account)
    this.revokeNotifyServerDeleteBook()
    return this.taskRecorder.finishSuccess(Unit)
  }

  override fun onFailure(result: TaskResult.Failure<Unit>) {
    this.publishBookStatus(BookStatus.FailedRevoke(this.bookID, result))
  }

  private fun debug(message: String, vararg arguments: Any?) =
    this.logger.debug("[{}] $message", this.bookID.brief(), *arguments)

  private fun error(message: String, vararg arguments: Any?) =
    this.logger.error("[{}] $message", this.bookID.brief(), *arguments)

  private fun warn(message: String, vararg arguments: Any?) =
    this.logger.warn("[{}] $message", this.bookID.brief(), *arguments)

  private fun publishBookStatus(status: BookStatus) {
    val book = this.bookFromDatabaseOrFallback()
    this.bookRegistry.update(BookWithStatus(book, status))
  }

  private fun publishStatusFromDatabase() {
    val book = this.bookFromDatabaseOrFallback()
    val status = BookStatus.fromBook(book)
    this.bookRegistry.update(BookWithStatus(book, status))
  }

  private fun bookFromDatabaseOrFallback(): Book {
    return if (this::databaseEntry.isInitialized) {
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
          OPDSAvailabilityLoanable.get()
        )
          .build()

      Book(
        this.bookID,
        this.accountID,
        null,
        null,
        entry,
        listOf()
      )
    }
  }

  private fun publishRequestingRevokeStatus() {
    this.publishBookStatus(BookStatus.RequestingRevoke(this.bookID))
  }

  private fun publishRevokedStatus() {
    this.publishBookStatus(BookStatus.Revoked(this.bookID))
  }

  private fun revokeNotifyServer(account: AccountType) {
    this.debug("notifying server of revocation")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeServerNotify)
    this.publishRequestingRevokeStatus()

    val feedEntry = this.databaseEntry.book.entry
    val availability = feedEntry.availability
    this.debug("availability is {}", availability)

    val newEntry = when (availability) {
      is OPDSAvailabilityHeldReady -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.HOLD, account)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
          this.feedEntryWithAvailability(accountID, feedEntry, OPDSAvailabilityLoanable.get())
        }
      }

      is OPDSAvailabilityHeld -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.HOLD, account)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
          this.feedEntryWithAvailability(accountID, feedEntry, OPDSAvailabilityHoldable.get())
        }
      }

      is OPDSAvailabilityHoldable -> {
        val exception = BookRevokeExceptionNotRevocable()
        val message =
          this.revokeStrings.revokeServerNotifyNotRevocable(availability.javaClass.simpleName)
        this.taskRecorder.currentStepFailed(message, "notRevocable", exception)
        throw TaskFailedHandled(exception)
      }

      is OPDSAvailabilityLoaned -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.LOAN, account)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
          this.feedEntryWithAvailability(accountID, feedEntry, OPDSAvailabilityLoanable.get())
        }
      }

      is OPDSAvailabilityLoanable -> {
        val exception = BookRevokeExceptionNotRevocable()
        val message =
          this.revokeStrings.revokeServerNotifyNotRevocable(availability.javaClass.simpleName)
        this.taskRecorder.currentStepFailed(message, "notRevocable", exception)
        throw TaskFailedHandled(exception)
      }

      is OPDSAvailabilityOpenAccess -> {
        val uriOpt = availability.revoke
        if (uriOpt is Some<URI>) {
          this.revokeNotifyServerURI(uriOpt.get(), RevokeType.LOAN, account)
        } else {
          this.debug("no revoke URI, nothing to do")
          this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeServerNotifyNoURI)
          FeedEntryOPDS(accountID, feedEntry)
        }
      }

      is OPDSAvailabilityRevoked ->
        this.revokeNotifyServerURI(availability.revoke, RevokeType.LOAN, account)

      else ->
        throw UnreachableCodeException()
    }

    this.revokeNotifyServerSaveNewEntry(newEntry)
  }

  private fun feedEntryWithAvailability(
    accountID: AccountID,
    oldEntry: OPDSAcquisitionFeedEntry,
    availability: OPDSAvailabilityType
  ): FeedEntryOPDS {
    val acquisitionFeedEntry =
      OPDSAcquisitionFeedEntry.newBuilderFrom(oldEntry)
        .setAvailability(availability)
        .build()

    return FeedEntryOPDS(accountID, acquisitionFeedEntry)
  }

  private fun revokeNotifyServerURI(
    targetURI: URI,
    revokeType: RevokeType,
    account: AccountType
  ): FeedEntryOPDS {
    this.debug("notifying server of {} revocation via {}", revokeType, targetURI)
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeServerNotifyURI(targetURI))
    this.publishRequestingRevokeStatus()

    val feed =
      this.revokeNotifyServerURIFeed(targetURI, account)
    val entry =
      this.revokeNotifyServerURIProcessFeed(feed)

    return entry
  }

  private fun revokeNotifyServerSaveNewEntry(entry: FeedEntryOPDS) {
    this.debug("saving received OPDS entry")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeServerNotifySavingEntry)
    this.publishRequestingRevokeStatus()

    try {
      this.databaseEntry.writeOPDSEntry(entry.feedEntry)
    } catch (e: Exception) {
      this.taskRecorder.currentStepFailed(
        this.revokeStrings.revokeServerNotifySavingEntryFailed, "unexpectedException", e
      )
      throw TaskFailedHandled(e)
    }
  }

  private fun revokeNotifyServerDeleteBook() {
    this.debug("deleting book")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeDeleteBook)
    this.publishRevokedStatus()
    this.publishStatusFromDatabase()
    this.databaseEntry.delete()
  }

  /*
   * XXX: Use [FeedLoading.loadSingleEntryFeed]
   */

  private fun revokeNotifyServerURIFeed(targetURI: URI, account: AccountType): Feed {
    val httpAuth = this.createHttpAuthIfRequired(account)

    /*
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    val feedResult = try {
      this.feedLoader.fetchURI(
        account.id,
        targetURI,
        httpAuth,
        "PUT"
      ).get(this.revokeServerTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
      val message = this.revokeStrings.revokeServerNotifyFeedTimedOut
      this.taskRecorder.currentStepFailed(message, "timedOut", e)
      throw TaskFailedHandled(e)
    } catch (e: ExecutionException) {
      val ex = e.cause!!
      if (ex is FeedHTTPTransportException) {
        this.taskRecorder.addAttributesIfPresent(ex.report?.toMap())
      }

      val message = this.revokeStrings.revokeServerNotifyFeedTimedOut
      this.taskRecorder.currentStepFailed(message, "feedLoaderFailed", ex)
      throw TaskFailedHandled(ex)
    }

    return when (feedResult) {
      is FeedLoaderSuccess -> {
        this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeServerNotifyFeedOK)
        feedResult.feed
      }

      is FeedLoaderFailedGeneral -> {
        val message = this.revokeStrings.revokeServerNotifyFeedFailed
        this.taskRecorder.addAttributesIfPresent(feedResult.problemReport?.toMap())
        this.taskRecorder.currentStepFailed(message, "feedLoaderFailed", feedResult.exception)
        throw TaskFailedHandled(feedResult.exception)
      }

      is FeedLoaderFailedAuthentication -> {
        val message = this.revokeStrings.revokeServerNotifyFeedFailed
        this.taskRecorder.addAttributesIfPresent(feedResult.problemReport?.toMap())
        this.taskRecorder.currentStepFailed(message, "feedLoaderFailed", feedResult.exception)
        throw TaskFailedHandled(feedResult.exception)
      }
    }
  }

  private fun revokeNotifyServerURIProcessFeed(feed: Feed): FeedEntryOPDS {
    this.debug("processing server revocation feed")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeServerNotifyProcessingFeed)
    this.publishRequestingRevokeStatus()

    if (feed.size == 0) {
      val exception = BookRevokeExceptionBadFeed()
      val message = this.revokeStrings.revokeServerNotifyFeedEmpty
      this.taskRecorder.currentStepFailed(message, "feedLoaderFailed", exception)
      throw TaskFailedHandled(exception)
    }

    return when (feed) {
      is Feed.FeedWithoutGroups -> {
        when (val feedEntry = feed.entriesInOrder[0]) {
          is FeedEntryCorrupt -> {
            val exception = BookRevokeExceptionBadFeed()
            val message = this.revokeStrings.revokeServerNotifyFeedCorrupt
            this.taskRecorder.currentStepFailed(message, "feedCorrupted", feedEntry.error)
            throw TaskFailedHandled(exception)
          }
          is FeedEntryOPDS ->
            feedEntry
        }
      }
      is Feed.FeedWithGroups -> {
        val exception = BookRevokeExceptionBadFeed()
        val message = this.revokeStrings.revokeServerNotifyFeedWithGroups
        this.taskRecorder.currentStepFailed(message, "feedUnusable", exception)
        throw TaskFailedHandled(exception)
      }
    }
  }

  private fun revokeFormatHandle(account: AccountType) {
    this.debug("revoking via format handle")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeFormat)
    this.publishRequestingRevokeStatus()

    return when (val handle = this.databaseEntry.findPreferredFormatHandle()) {
      is BookDatabaseEntryFormatHandleEPUB ->
        this.revokeFormatHandleEPUB(handle, account)
      is BookDatabaseEntryFormatHandlePDF ->
        this.revokeFormatHandlePDF(handle)
      is BookDatabaseEntryFormatHandleAudioBook ->
        this.revokeFormatHandleAudioBook(handle)
      null -> {
        this.debug("no format handle available, nothing to do!")
        this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeFormatNothingToDo)
        Unit
      }
    }
  }

  private fun revokeFormatHandleEPUB(handle: BookDatabaseEntryFormatHandleEPUB, account: AccountType) {
    this.debug("revoking via EPUB format handle")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeFormatSpecific("EPUB"))
    this.publishRequestingRevokeStatus()

    return when (val drm = handle.drmInformationHandle) {
      is BookDRMInformationHandle.ACSHandle -> {
        val adobeRights = drm.info.rights
        if (adobeRights != null) {
          this.revokeFormatHandleEPUBAdobe(handle, adobeRights.second, account)
        } else {
          this.debug("no Adobe rights, nothing to do!")
        }
      }
      is BookDRMInformationHandle.LCPHandle,
      is BookDRMInformationHandle.AxisHandle,
      is BookDRMInformationHandle.NoneHandle -> {
        // Nothing required
      }
    }
  }

  private fun revokeFormatHandleEPUBAdobe(
    handle: BookDatabaseEntryFormatHandleEPUB,
    adobeRights: AdobeAdeptLoan,
    account: AccountType
  ) {
    this.debug("revoking Adobe ACS loan")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeACSLoan)
    this.publishRequestingRevokeStatus()

    /*
     * If the loan is not returnable, then there's no point trying to return it!
     */

    if (!adobeRights.isReturnable) {
      this.debug("loan is not returnable")
      this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeACSLoanNotReturnable)
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
      this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeACSLoanNotSupported)
      this.deleteAdobeRights(handle)
      return
    }

    this.revokeFormatHandleEPUBAdobeExecute(this.adobeDRM, adobeRights, account)
    this.deleteAdobeRights(handle)
  }

  /**
   * Execute the DRM connector commands required to revoke a loan.
   */

  private fun revokeFormatHandleEPUBAdobeExecute(
    adobeDRM: AdobeAdeptExecutorType,
    adobeRights: AdobeAdeptLoan,
    account: AccountType
  ) {
    val credentials =
      this.revokeFormatHandleEPUBAdobeWithConnectorGetCredentials(account)

    this.taskRecorder.beginNewStep(this.revokeStrings.revokeACSExecute)
    this.publishRequestingRevokeStatus()

    val adeptFuture =
      AdobeDRMExtensions.revoke(adobeDRM, adobeRights, credentials.userID)

    try {
      adeptFuture.get(this.revokeACSTimeoutDuration.standardSeconds, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
      val message = this.revokeStrings.revokeACSTimedOut
      this.taskRecorder.currentStepFailed(message, "timedOut", e)
      throw TaskFailedHandled(e)
    } catch (e: ExecutionException) {
      throw when (val cause = e.cause!!) {
        is CancellationException -> {
          val message = this.revokeStrings.revokeBookCancelled
          this.taskRecorder.currentStepFailed(message, "cancelled", cause)
          TaskFailedHandled(cause)
        }
        is AdobeDRMExtensions.AdobeDRMRevokeException -> {
          val message = this.revokeStrings.revokeBookACSConnectorFailed(cause.errorCode)
          this.taskRecorder.currentStepFailed(message, "${this.adobeACS}: ${cause.errorCode}", cause)
          TaskFailedHandled(cause)
        }
        else -> {
          this.taskRecorder.currentStepFailed(this.revokeStrings.revokeBookACSFailed, "unexpectedException", cause)
          TaskFailedHandled(cause)
        }
      }
    } catch (e: Throwable) {
      this.taskRecorder.currentStepFailed(this.revokeStrings.revokeBookACSFailed, "unexpectedException", e)
      throw TaskFailedHandled(e)
    }

    this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeACSExecuteOK)
  }

  /**
   * Retrieve the post-activation device credentials. These can only exist if the device
   * has been activated.
   */

  private fun revokeFormatHandleEPUBAdobeWithConnectorGetCredentials(account: AccountType): AccountAuthenticationAdobePostActivationCredentials {
    this.debug("getting Adobe ACS credentials")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeACSGettingDeviceCredentials)
    this.publishRequestingRevokeStatus()

    val credentials =
      this.getRequiredAccountCredentials(account).adobeCredentials?.postActivationCredentials

    if (credentials == null) {
      val exception = BookRevokeExceptionDeviceNotActivated()
      val message = this.revokeStrings.revokeACSGettingDeviceCredentialsNotActivated
      this.taskRecorder.currentStepFailed(message, "${this.adobeACS}: drmDeviceNotActive", exception)
      throw TaskFailedHandled(exception)
    }

    this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeACSGettingDeviceCredentialsOK)
    return credentials
  }

  private fun deleteAdobeRights(handle: BookDatabaseEntryFormatHandleEPUB) {
    this.debug("deleting Adobe ACS loan")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeACSDeleteRights)
    this.publishRequestingRevokeStatus()

    try {
      when (val drm = handle.drmInformationHandle) {
        is BookDRMInformationHandle.ACSHandle ->
          drm.setAdobeRightsInformation(null)
        is BookDRMInformationHandle.LCPHandle,
        is BookDRMInformationHandle.NoneHandle -> {
          // Nothing required
        }
      }
    } catch (e: Exception) {
      this.taskRecorder.currentStepFailed(
        this.revokeStrings.revokeACSDeleteRightsFailed, "unexpectedException", e
      )
      throw TaskFailedHandled(e)
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun revokeFormatHandleAudioBook(handle: BookDatabaseEntryFormatHandleAudioBook) {
    this.debug("revoking via AudioBook format handle")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeFormatSpecific("AudioBook"))
    this.publishRequestingRevokeStatus()

    this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeFormatNothingToDo)
  }

  @Suppress("UNUSED_PARAMETER")
  private fun revokeFormatHandlePDF(handle: BookDatabaseEntryFormatHandlePDF) {
    this.debug("revoking via PDF format handle")
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeFormatSpecific("PDF"))
    this.publishRequestingRevokeStatus()

    this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeFormatNothingToDo)
  }

  private fun setupBookDatabaseEntry(account: AccountType) {
    this.taskRecorder.beginNewStep(this.revokeStrings.revokeBookDatabaseLookup)

    try {
      this.debug("setting up book database entry")
      val database = account.bookDatabase
      this.databaseEntry = database.entry(this.bookID)
      this.publishRequestingRevokeStatus()
      this.taskRecorder.currentStepSucceeded(this.revokeStrings.revokeBookDatabaseLookupOK)
    } catch (e: Exception) {
      this.error("failed to set up book database entry: ", e)
      this.taskRecorder.currentStepFailed(
        this.revokeStrings.revokeBookDatabaseLookupFailed, "unexpectedException", e
      )
      throw TaskFailedHandled(e)
    }
  }

  private enum class RevokeType {
    LOAN, HOLD
  }
}
