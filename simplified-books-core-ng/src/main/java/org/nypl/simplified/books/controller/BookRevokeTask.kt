package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType
import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP
import org.nypl.simplified.books.accounts.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.Book
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.exceptions.AccountNotReadyException
import org.nypl.simplified.books.exceptions.BookRevokeExceptionDRMWorkflowError
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoCredentials
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNotReady
import org.nypl.simplified.books.feeds.Feed
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSAvailabilityType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class BookRevokeTask(
  private val adobeDRM: AdobeAdeptExecutorType?,
  private val bookRegistry: BookRegistryType,
  private val feedLoader: FeedLoaderType,
  private val account: AccountType,
  private val bookID: BookID) : Callable<Unit> {

  @Throws(Exception::class)
  override fun call(): Unit {
    LOG.debug("[{}] revoke", this.bookID.brief())

    val databaseEntry = this.account.bookDatabase().entry(this.bookID)
    val book = databaseEntry.book

    try {
      this.bookRegistry.update(BookWithStatus.create(book, BookStatusRequestingRevoke(this.bookID)))

      val feedEntry = book.entry
      val avail = feedEntry.availability
      LOG.debug("[{}]: availability is {}", this.bookID.brief(), avail)

      return avail.matchAvailability(object : OPDSAvailabilityMatcherType<Unit, Exception> {
        private fun optionallyRevokeHeld(revokeOpt: OptionType<URI>?): Unit {
          if (revokeOpt is Some<URI>) {
            return revokeUsingOnlyURI(revokeOpt.get(), RevokeType.HOLD)
          }
          return Unit.unit()
        }

        override fun onHeldReady(availability: OPDSAvailabilityHeldReady): Unit {
          return optionallyRevokeHeld(availability.revoke)
        }

        override fun onHeld(availability: OPDSAvailabilityHeld): Unit {
          return optionallyRevokeHeld(availability.revoke)
        }

        override fun onHoldable(availability: OPDSAvailabilityHoldable): Unit {
          return failBecauseNotRevocable(book, availability)
        }

        override fun onLoaned(availability: OPDSAvailabilityLoaned): Unit {
          val revokeOpt = availability.revoke
          if (revokeOpt is Some<URI>) {
            return revokeLoaned(databaseEntry, revokeOpt.get())
          } else {
            return failBecauseNotRevocable(book, availability)
          }
        }

        override fun onLoanable(availability: OPDSAvailabilityLoanable): Unit {
          return failBecauseNotRevocable(book, availability)
        }

        override fun onOpenAccess(availability: OPDSAvailabilityOpenAccess): Unit {
          val revokeOpt = availability.revoke
          if (revokeOpt is Some<URI>) {
            return revokeLoaned(databaseEntry, revokeOpt.get())
          }
          return failBecauseNotRevocable(book, availability)
        }

        override fun onRevoked(availability: OPDSAvailabilityRevoked): Unit {
          return revokeUsingOnlyURI(availability.revoke, RevokeType.LOAN)
        }
      })
    } catch (e: Exception) {
      this.revokeFailed(book, Option.some(e), e.message!!)
      throw e
    } finally {
      LOG.debug("[{}] revoke finished", this.bookID.brief())
    }
  }

  @Throws(Exception::class)
  private fun revokeUsingOnlyURI(
    revokeURI: URI,
    type: RevokeType): Unit {

    LOG.debug("[{}]: revoking URI {} of type {}", this.bookID.brief(), revokeURI, type)

    val httpAuth = createHttpAuthIfRequired()

    /*
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    val feedResult =
      this.feedLoader.fetchURIRefreshing(revokeURI, httpAuth, "PUT")
        .get(3L, TimeUnit.MINUTES)

    return when (feedResult) {
      is FeedLoaderSuccess ->
        revokeUsingOnlyURIReceivedFeed(feedResult.feed)
      is FeedLoaderFailedGeneral ->
        throw feedResult.exception
      is FeedLoaderFailedAuthentication ->
        throw feedResult.exception
    }
  }

  private fun revokeUsingOnlyURIReceivedFeed(feed: Feed): Unit {
    if (feed.size == 0) {
      throw IOException("Received empty feed")
    }

    return when (feed) {
      is Feed.FeedWithoutGroups -> {
        val feedEntry = feed.entriesInOrder[0]
        when (feedEntry) {
          is FeedEntryCorrupt ->
            throw IOException("Received a corrupted feed")
          is FeedEntryOPDS ->
            this.revokeFeedEntryReceivedOPDS(feedEntry)
        }
      }
      is Feed.FeedWithGroups ->
        throw IOException("Received an unexpected type of feed (feed with groups)")
    }
  }

  /**
   * If the account requires credentials, create HTTP auth details. If no credentials
   * are provided, throw an exception.
   */

  private fun createHttpAuthIfRequired(): OptionType<HTTPAuthType> {
    return if (this.account.requiresCredentials()) {
      Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(getRequiredAccountCredentials()))
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
      LOG.error("[{}] revocation requires credentials, but none are available", this.bookID.brief())
      throw BookRevokeExceptionNoCredentials()
    }
  }

  @Throws(Exception::class)
  private fun revokeLoaned(
    databaseEntry: BookDatabaseEntryType,
    revokeURI: URI): Unit {

    val formatHandle = databaseEntry.findPreferredFormatHandle()
    return if (formatHandle != null) {
      when (formatHandle) {
        is BookDatabaseEntryFormatHandleEPUB ->
          revokeLoanedEPUB(databaseEntry, formatHandle, revokeURI)
        is BookDatabaseEntryFormatHandleAudioBook ->
          revokeLoanedAudioBook(formatHandle, revokeURI)
        is BookDatabaseEntryFormatHandlePDF ->
          revokeLoanedPDF(formatHandle, revokeURI)
      }
    } else {
      throw UnreachableCodeException()
    }
  }

  private fun revokeLoanedPDF(
    formatHandle: BookDatabaseEntryFormatHandlePDF,
    revokeURI: URI): Unit {
    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  private fun revokeLoanedAudioBook(
    formatHandle: BookDatabaseEntryFormatHandleAudioBook,
    revokeURI: URI): Unit {
    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  private fun revokeLoanedEPUB(
    databaseEntry: BookDatabaseEntryType,
    formatHandle: BookDatabaseEntryFormatHandleEPUB,
    revokeURI: URI): Unit {

    val adobeRights = formatHandle.format.adobeRights
    if (adobeRights == null) {

      /*
       * If the Adobe loan information is gone, it's assumed that it is a non-drm
       * book from a library that still needs to be "returned"
       */

      return revokeLoanedEPUBWithoutDRM(databaseEntry, revokeURI)
    }

    if (this.adobeDRM == null) {
      throw java.lang.IllegalStateException(
        "Loan has Adobe rights information, but DRM is not supported!")
    }

    /*
     * If it turns out that the loan is not actually returnable, well, there's
     * nothing we can do about that. This is a bug in the program.
     */

    if (adobeRights.isReturnable) {
      this.revokeLoanedEPUBReturnAdobeLoan(databaseEntry, this.adobeDRM, adobeRights, revokeURI)
    }

    /*
     * Everything went well... Finish the revocation by telling the server about it.
     */

    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  @Throws(IOException::class)
  private fun revokeLoanedEPUBWithoutDRM(
    databaseEntry: BookDatabaseEntryType,
    revokeURI: URI): Unit {

    /*
     * Save the "revoked" state of the book.
     * Finish the revocation by telling the server about it.
     */

    val b = OPDSAcquisitionFeedEntry.newBuilderFrom(databaseEntry.book.entry)
    b.setAvailability(OPDSAvailabilityRevoked.get(revokeURI))
    val ee = b.build()

    databaseEntry.writeOPDSEntry(ee)
    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  private fun revokeLoanedEPUBReturnAdobeLoan(
    databaseEntry: BookDatabaseEntryType,
    adobe: AdobeAdeptExecutorType,
    adobeLoan: AdobeAdeptLoan,
    revokeURI: URI) {

    val accountCredentials = getRequiredAccountCredentials()

    /*
     * Execute a task using the Adobe DRM library, and wait for it to
     * finish. The reason for the waiting, as opposed to calling further
     * methods from inside the listener callbacks is to avoid any chance
     * of the methods in question propagating an unchecked exception back
     * to the native code. This will obviously crash the whole process,
     * rather than just failing the revocation.
     */

    val latch = CountDownLatch(1)
    val listener = AdobeLoanReturnResult(latch)
    adobe.execute { connector ->
      val adobeUserOpt = accountCredentials.adobePostActivationCredentials()
      val creds = (adobeUserOpt as Some<AccountAuthenticationAdobePostActivationCredentials>).get()
      connector.loanReturn(listener, adobeLoan.id, creds.userID())
    }

    /*
     * Wait for the Adobe task to finish. Give up if it appears to be
     * hanging.
     */

    try {
      latch.await(3, TimeUnit.MINUTES)
    } catch (x: InterruptedException) {
      throw IOException("Timed out waiting for Adobe revocation!", x)
    }

    /*
     * If Adobe couldn't revoke the book, then the book isn't revoked.
     * The user can try again later.
     */

    val errorOpt = listener.error
    if (errorOpt is Some<Throwable>) {
      this.revokeFailed(databaseEntry.book, errorOpt, errorOpt.get().localizedMessage)
      throw errorOpt.get()
    }

    /*
     * Save the "revoked" state of the book.
     */

    val b = OPDSAcquisitionFeedEntry.newBuilderFrom(databaseEntry.book.entry)
    b.setAvailability(OPDSAvailabilityRevoked.get(revokeURI))
    val ee = b.build()
    databaseEntry.writeOPDSEntry(ee)
  }

  /**
   * An entry was received regarding the current state of the book. Publish a
   * status value so that any views that are still looking at the book can
   * re-render themselves with the new information, then delete the local book data.
   */

  @Throws(IOException::class)
  private fun revokeFeedEntryReceivedOPDS(entry: FeedEntryOPDS): Unit {
    LOG.debug("[{}] deleting book and publishing revocation status", this.bookID.brief())

    val databaseEntry = this.account.bookDatabase().entry(this.bookID)
    databaseEntry.delete()

    this.bookRegistry.clearFor(this.bookID)
    return Unit.unit()
  }

  private enum class RevokeType {
    LOAN, HOLD
  }

  private fun failBecauseNotRevocable(book: Book, availability: OPDSAvailabilityType): Unit {
    return this.revokeFailed(
      book, Option.none(), String.format("Status is %s, nothing to revoke!", availability))
  }

  /**
   * The revocation failed.
   */

  private fun revokeFailed(
    book: Book,
    exception: OptionType<Throwable>,
    message: String): Unit {
    LOG.error("[{}] revocation failed: ", this.bookID.brief(), message)

    if (exception.isSome) {
      val ex = (exception as Some<Throwable>).get()
      LOG.error("[{}] revocation failed, exception: ", this.bookID.brief(), ex)
    }

    LOG.debug("[{}] publishing failure status", this.bookID.brief())
    this.bookRegistry.update(
      BookWithStatus.create(book, BookStatusRevokeFailed(this.bookID, exception)))
    return Unit.unit()
  }

  private class AdobeLoanReturnResult internal constructor(
    private val latch: CountDownLatch) : AdobeAdeptLoanReturnListenerType {

    var error: OptionType<Throwable> = Option.some(BookRevokeExceptionNotReady())

    override fun onLoanReturnSuccess() {
      try {
        LOG.debug("onLoanReturnSuccess")
        this.error = Option.none()
      } finally {
        this.latch.countDown()
      }
    }

    override fun onLoanReturnFailure(in_error: String) {
      try {
        LOG.debug("onLoanReturnFailure: {}", in_error)

        if (in_error.startsWith("E_ACT_NOT_READY")) {
          this.error = Option.some(AccountNotReadyException(in_error))
        } else if (in_error.startsWith("E_STREAM_ERROR")) {
          LOG.debug("E_STREAM_ERROR: Ignore and continue with return.")
          this.error = Option.none()
        } else {
          this.error = Option.some(BookRevokeExceptionDRMWorkflowError(in_error))
        }
      } finally {
        this.latch.countDown()
      }
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(BookRevokeTask::class.java)
  }
}
