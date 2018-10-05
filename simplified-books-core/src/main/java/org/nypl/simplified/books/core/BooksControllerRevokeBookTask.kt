package org.nypl.simplified.books.core

import android.content.Context
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType
import org.nypl.drm.core.AdobeUserID
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
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

internal class BooksControllerRevokeBookTask(
  private val context: Context,
  private val booksDatabase: BookDatabaseType,
  private val accountsDatabase: AccountsDatabaseReadableType,
  private val booksStatus: BooksStatusCacheType,
  private val feedLoader: FeedLoaderType,
  private val bookID: BookID,
  private val adobeDRM: OptionType<AdobeAdeptExecutorType>,
  private val needsAuthentication: Boolean) : Callable<Unit> {

  private lateinit var databaseEntry: BookDatabaseEntryType

  override fun call(): Unit {
    try {
      LOG.debug("[{}]: revoking", this.bookID.shortID)

      this.databaseEntry = this.booksDatabase.databaseOpenExistingEntry(this.bookID)
      val (_, _, entry) = this.databaseEntry.entryGetSnapshot()

      val avail = entry.availability
      LOG.debug("[{}]: availability is {}", this.bookID.shortID, avail)

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
          return failBecauseNotRevocable(availability)
        }

        override fun onLoaned(availability: OPDSAvailabilityLoaned): Unit {
          val revokeOpt = availability.revoke
          if (revokeOpt is Some<URI>) {
            return revokeLoaned(revokeOpt.get())
          } else {
            return failBecauseNotRevocable(availability)
          }
        }

        override fun onLoanable(availability: OPDSAvailabilityLoanable): Unit {
          return failBecauseNotRevocable(availability)
        }

        override fun onOpenAccess(availability: OPDSAvailabilityOpenAccess): Unit {
          val revokeOpt = availability.revoke
          if (revokeOpt is Some<URI>) {
            return revokeLoaned(revokeOpt.get())
          }
          return Unit.unit()
        }

        override fun onRevoked(availability: OPDSAvailabilityRevoked): Unit {
          return revokeUsingOnlyURI(availability.revoke, RevokeType.LOAN)
        }
      })
    } catch (ex: Throwable) {
      LOG.error("[{}]: could not revoke book: ", this.bookID.shortID, ex)
      this.revokeFailed(Option.some(ex), ex.message)
      throw ex
    }
  }

  @Throws(Exception::class)
  private fun revokeUsingOnlyURI(
    revokeURI: URI,
    type: RevokeType): Unit {

    LOG.debug("[{}]: revoking URI {} of type {}", this.bookID.shortID, revokeURI, type)

    val accountCredentials = findAccountCredentials()
    val httpAuth = AccountCredentialsHTTP.toHttpAuth(accountCredentials)

    /*
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    val feed =
      this.feedLoader.fetchURIRefreshing(revokeURI, Option.some(httpAuth), "PUT")
        .get(3L, TimeUnit.MINUTES)

    if (feed is FeedWithoutGroups) {
      if (feed.size == 0) {
        throw IOException("Received empty feed")
      }
      val feedEntry = feed[0]
      if (feedEntry is FeedEntryOPDS) {
        return this.revokeFeedEntryReceivedOPDS(feedEntry)
      } else {
        throw IOException("Received an unexpected type of feed entry: " + feedEntry)
      }
    } else {
      throw IOException("Received an unexpected type of feed: " + feed)
    }
  }

  /**
   * An entry was received regarding the current state of the book. Publish a
   * status value so that any views that are still looking at the book can
   * re-render themselves with the new information, then delete the local book data.
   */

  @Throws(IOException::class)
  private fun revokeFeedEntryReceivedOPDS(entry: FeedEntryOPDS): Unit {
    LOG.debug("[{}]: publishing revocation status", this.bookID.shortID)

    this.booksStatus.booksRevocationFeedEntryUpdate(entry)

    BooksControllerDeleteBookDataTask(
      this.context,
      this.booksStatus,
      this.booksDatabase,
      this.bookID,
      this.needsAuthentication)
      .call()

    this.booksStatus.booksStatusClearFor(this.bookID)
    this.databaseEntry.entryDestroy()
    return Unit.unit()
  }

  /**
   * Revocation failed.
   */

  private fun revokeFailed(
    error: OptionType<Throwable>,
    message: String?): Unit {
    LOG.error("[{}]: revocation failed: {}", this.bookID.shortID, message)

    if (error is Some<Throwable>) {
      LOG.error("[{}]: revocation failed, exception: ", this.bookID.shortID, error.get())
    }

    LOG.debug("[{}] publishing failure status", this.bookID.shortID)
    val status = BookStatusRevokeFailed(this.bookID, error)
    this.booksStatus.booksStatusUpdate(status)
    return Unit.unit()
  }

  private fun failBecauseNotRevocable(availability: OPDSAvailabilityType): Unit {
    return this.revokeFailed(
      Option.none(), String.format("Status is %s, nothing to revoke!", availability))
  }

  @Throws(Exception::class)
  private fun revokeLoaned(revokeURI: URI): Unit {

    val formatHandleOpt =
      this.databaseEntry.entryFindPreferredFormatHandle()

    return if (formatHandleOpt is Some<BookDatabaseEntryFormatHandle>) {
      val formatHandle = formatHandleOpt.get()
      when (formatHandle) {
        is BookDatabaseEntryFormatHandleEPUB -> revokeLoanedEPUB(formatHandle, revokeURI)
        is BookDatabaseEntryFormatHandleAudioBook -> revokeLoanedAudioBook(formatHandle, revokeURI)
      }
    } else {
      throw UnreachableCodeException()
    }
  }

  private fun revokeLoanedAudioBook(
    formatHandle: BookDatabaseEntryFormatHandleAudioBook,
    revokeURI: URI): Unit {
    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  private fun revokeLoanedEPUB(
    formatHandle: BookDatabaseEntryFormatHandleEPUB,
    revokeURI: URI): Unit {

    val snapshot = this.databaseEntry.entryGetSnapshot()

    val formatSnapshotEPUBOpt = snapshot.findFormat(BookDatabaseEntryFormatSnapshotEPUB::class.java)
    if (!(formatSnapshotEPUBOpt is Some<BookDatabaseEntryFormatSnapshotEPUB>)) {
      throw java.lang.IllegalStateException("No format handle snapshot in database entry!")
    }

    val formatSnapshotEPUB = formatSnapshotEPUBOpt.get()
    if (!(formatSnapshotEPUB.adobeRights is Some<AdobeAdeptLoan>)) {

      /*
       * If the Adobe loan information is gone, it's assumed that it is a non-drm
       * book from a library that still needs to be "returned"
       */

      return revokeLoanedEPUBWithoutDRM(snapshot, revokeURI)
    }

    val adobeLoan = formatSnapshotEPUB.adobeRights.get()
    if (!(this.adobeDRM is Some<AdobeAdeptExecutorType>)) {
      throw java.lang.IllegalStateException(
        "Loan has Adobe rights information, but DRM is not supported!")
    }

    val adobe = this.adobeDRM.get()

    /*
     * If it turns out that the loan is not actually returnable, well, there's
     * nothing we can do about that. This is a bug in the program.
     */

    if (adobeLoan.isReturnable) {
      this.revokeLoanedEPUBReturnAdobeLoan(adobe, adobeLoan, snapshot, revokeURI)
    }

    /*
     * Everything went well... Finish the revocation by telling the server about it.
     */

    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  private fun revokeLoanedEPUBReturnAdobeLoan(
    adobe: AdobeAdeptExecutorType,
    adobeLoan: AdobeAdeptLoan,
    snapshot: BookDatabaseEntrySnapshot,
    revokeURI: URI) {

    val accountCredentials = findAccountCredentials()

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
      val adobeUserOpt = accountCredentials.adobeUserID
      val user = (adobeUserOpt as Some<AdobeUserID>).get()
      connector.loanReturn(listener, adobeLoan.id, user)
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
      this.revokeFailed(errorOpt, null)
      throw errorOpt.get()
    }

    /*
     * Save the "revoked" state of the book.
     */

    val b = OPDSAcquisitionFeedEntry.newBuilderFrom(snapshot.entry)
    b.setAvailability(OPDSAvailabilityRevoked.get(revokeURI))
    val ee = b.build()
    this.databaseEntry.entrySetFeedData(ee)
  }

  private fun findAccountCredentials(): AccountCredentials {
    val accountCredentialsOpt = this.accountsDatabase.accountGetCredentials()
    if (!(accountCredentialsOpt is Some<AccountCredentials>)) {
      throw IllegalStateException("Not logged in!")
    }

    val accountCredentials = accountCredentialsOpt.get()
    return accountCredentials
  }

  @Throws(IOException::class)
  private fun revokeLoanedEPUBWithoutDRM(
    snapshot: BookDatabaseEntrySnapshot,
    revokeURI: URI): Unit {

    /*
     * Save the "revoked" state of the book.
     * Finish the revocation by telling the server about it.
     */

    val b = OPDSAcquisitionFeedEntry.newBuilderFrom(snapshot.entry)
    b.setAvailability(OPDSAvailabilityRevoked.get(revokeURI))
    val ee = b.build()

    this.databaseEntry.entrySetFeedData(ee)
    return this.revokeUsingOnlyURI(revokeURI, RevokeType.LOAN)
  }

  private enum class RevokeType {
    LOAN, HOLD
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
        }// Known issue of 404 URL for OneClick/RBdigital books

      } finally {
        this.latch.countDown()
      }
    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(BooksControllerRevokeBookTask::class.java)
  }
}
