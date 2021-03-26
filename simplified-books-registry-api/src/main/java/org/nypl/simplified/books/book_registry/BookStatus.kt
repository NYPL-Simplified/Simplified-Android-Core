package org.nypl.simplified.books.book_registry

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.DateTime
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.net.URI

sealed class BookStatus {

  /**
   * @return The status priority; higher priority status updates will replace
   * lower priority values.
   */

  abstract val priority: BookStatusPriorityOrdering

  /**
   * @return The unique identifier of the book
   */

  abstract val id: BookID

  /**
   * The book is placed on hold.
   */

  sealed class Held : BookStatus() {

    /**
     * @return The approximate date that the book will become available
     */

    abstract val endDate: DateTime?

    /**
     * @return `true` iff the hold is revocable
     */

    abstract val isRevocable: Boolean

    /**
     * The given book is currently placed on hold.
     */

    data class HeldInQueue(
      override val id: BookID,

      /**
       * The current position of the user in the queue
       */

      val queuePosition: Int?,

      /**
       * @return The current position of the user in the queue
       */

      val startDate: DateTime?,

      override val isRevocable: Boolean,
      override val endDate: DateTime?
    ) : Held() {

      override val priority: BookStatusPriorityOrdering
        get() = BookStatusPriorityOrdering.BOOK_STATUS_HELD
    }

    /**
     * The given book is currently reserved for the user. This is equivalent to a
     * hold where the current user is at position 0 in the queue.
     */

    data class HeldReady(
      override val id: BookID,
      override val endDate: DateTime?,

      /**
       * @return `true` if the hold is revocable
       */

      override val isRevocable: Boolean
    ) : Held() {

      override val priority: BookStatusPriorityOrdering
        get() = BookStatusPriorityOrdering.BOOK_STATUS_HELD_READY
    }
  }

  /**
   * The given book could not be revoked (hold cancelled, loan returned, etc).
   */

  data class FailedRevoke(

    /**
     * The book ID
     */

    override val id: BookID,

    /**
     * The list of steps that lead to the failure.
     */

    val result: TaskResult.Failure<Unit>
  ) : BookStatus(), PresentableErrorType {

    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_REVOKE_FAILED

    override val message: String =
      this.result.message
    override val exception: Throwable? =
      this.result.exception
    override val attributes: Map<String, String> =
      this.result.attributes
  }

  /**
   * The given book failed to download properly.
   */

  data class FailedDownload(
    override val id: BookID,

    /**
     * The list of steps that lead to the failure.
     */

    val result: TaskResult.Failure<Unit>
  ) : BookStatus(), PresentableErrorType {

    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_FAILED

    override val message: String =
      this.result.message
    override val exception: Throwable? =
      this.result.exception
    override val attributes: Map<String, String> =
      this.result.attributes
  }

  /**
   * The given book failed to loan properly.
   */

  data class FailedLoan(
    override val id: BookID,

    /**
     * The list of steps that lead to the failure.
     */

    val result: TaskResult.Failure<Unit>
  ) : BookStatus(), PresentableErrorType {

    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_FAILED

    override val message: String =
      this.result.message
    override val exception: Throwable? =
      this.result.exception
    override val attributes: Map<String, String> =
      this.result.attributes
  }

  /**
   * The given book not available for loan, but may be placed on hold.
   */

  data class Holdable(
    override val id: BookID
  ) : BookStatus() {

    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_HOLDABLE
  }

  /**
   * A status value indicating that a book can be loaned.
   */

  data class Loanable(
    override val id: BookID
  ) : BookStatus() {

    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_LOANABLE
  }

  /**
   * The given book is owned/loaned.
   */

  sealed class Loaned : BookStatus() {

    /**
     * The approximate date/time that the loan expires.
     */

    abstract val loanExpiryDate: DateTime?

    /**
     * `true` if the loan is returnable
     */

    abstract val returnable: Boolean

    /**
     * The given book is owned/loaned but is not downloaded and is therefore not
     * ready for reading.
     */

    data class LoanedNotDownloaded(
      override val id: BookID,
      override val loanExpiryDate: DateTime?,
      override val returnable: Boolean
    ) : Loaned() {

      override val priority: BookStatusPriorityOrdering
        get() = BookStatusPriorityOrdering.BOOK_STATUS_LOANED
    }

    /**
     * The given book is downloaded and available for reading.
     */

    data class LoanedDownloaded(
      override val id: BookID,
      override val loanExpiryDate: DateTime?,
      override val returnable: Boolean
    ) : Loaned() {

      override val priority: BookStatusPriorityOrdering
        get() = BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOADED
    }
  }

  /**
   * The loan is currently being revoked.
   */

  data class RequestingRevoke(
    override val id: BookID
  ) : BookStatus() {
    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_REVOKE_IN_PROGRESS
  }

  /**
   * The given book is being requested but it is not yet known if the book is
   * loaned or not.
   */

  data class RequestingLoan(
    override val id: BookID,
    val detailMessage: String
  ) : BookStatus() {
    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_LOAN_IN_PROGRESS
  }

  /**
   * The given book is being requested for download, but the download has not
   * actually started yet.
   */

  data class RequestingDownload(
    override val id: BookID
  ) : BookStatus() {
    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_REQUESTING
  }

  /**
   * The given book is currently downloading.
   */

  data class Downloading(
    override val id: BookID,

    /**
     * The current number of downloaded bytes
     */

    val currentTotalBytes: Long?,

    /**
     * The expected total bytes
     */

    val expectedTotalBytes: Long?,
    val detailMessage: String
  ) : BookStatus() {

    val progressPercent: Double? =
      this.currentTotalBytes?.let { currentTotalBytes ->
        val expectedTotalBytes = this.expectedTotalBytes ?: 100.0
        (currentTotalBytes.toDouble() / expectedTotalBytes.toDouble()) * 100.0
      }

    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_IN_PROGRESS
  }

  /**
   * The given book is downloading, and external authentication has been required by the provider.
   */

  data class DownloadWaitingForExternalAuthentication(
    override val id: BookID,
    val downloadURI: URI
  ) : BookStatus() {
    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_WAITING_FOR_EXTERNAL_AUTHENTICATION
  }

  /**
   * The given book is downloading, and a required external authentication is in progress.
   */

  data class DownloadExternalAuthenticationInProgress(
    override val id: BookID
  ) : BookStatus() {
    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_EXTERNAL_AUTHENTICATION_IN_PROGRESS
  }

  /**
   * The given book is revoked, but has not yet been removed from the database. A given
   * book is expected to spend very little time in this state.
   */

  data class Revoked(
    override val id: BookID
  ) : BookStatus() {
    override val priority: BookStatusPriorityOrdering
      get() = BookStatusPriorityOrdering.BOOK_STATUS_LOANED
  }

  companion object {

    fun fromBook(book: Book): BookStatus {
      val downloaded = book.isDownloaded
      val drmReturnable = this.isDRMReturnable(book)
      val availability = book.entry.availability
      return availability.matchAvailability(
        object : OPDSAvailabilityMatcherType<BookStatus, UnreachableCodeException> {
          override fun onHeldReady(a: OPDSAvailabilityHeldReady): BookStatus {
            return this@Companion.onIsHeldReady(a, book)
          }

          override fun onHeld(a: OPDSAvailabilityHeld): BookStatus {
            return this@Companion.onIsHeldNotReady(a, book)
          }

          override fun onHoldable(a: OPDSAvailabilityHoldable): BookStatus {
            return this@Companion.onIsHoldable(book)
          }

          override fun onLoaned(a: OPDSAvailabilityLoaned): BookStatus {
            return this@Companion.onIsLoaned(a, drmReturnable, downloaded, book)
          }

          override fun onLoanable(a: OPDSAvailabilityLoanable): BookStatus {
            return this@Companion.onIsLoanable(book)
          }

          override fun onOpenAccess(a: OPDSAvailabilityOpenAccess): BookStatus {
            return this@Companion.onIsOpenAccess(a, downloaded, book)
          }

          override fun onRevoked(a: OPDSAvailabilityRevoked): BookStatus {
            return this@Companion.onIsRevoked(book)
          }
        })
    }

    private fun onIsRevoked(
      book: Book
    ): BookStatus {
      return Revoked(book.id)
    }

    private fun onIsOpenAccess(
      a: OPDSAvailabilityOpenAccess,
      downloaded: Boolean,
      book: Book
    ): BookStatus {
      return if (downloaded) {
        Loaned.LoanedDownloaded(
          id = book.id,
          loanExpiryDate = this.someOrNull(a.endDate),
          returnable = a.revoke.isSome
        )
      } else {
        Loaned.LoanedNotDownloaded(
          id = book.id,
          loanExpiryDate = this.someOrNull(a.endDate),
          returnable = a.revoke.isSome
        )
      }
    }

    private fun onIsLoanable(book: Book): BookStatus {
      return Loanable(book.id)
    }

    private fun onIsLoaned(
      a: OPDSAvailabilityLoaned,
      drmReturnable: Boolean,
      downloaded: Boolean,
      book: Book
    ): BookStatus {
      val hasRevoke = a.revoke.isSome
      val returnable = hasRevoke && drmReturnable || hasRevoke && downloaded
      return if (downloaded) {
        Loaned.LoanedDownloaded(
          id = book.id,
          loanExpiryDate = this.someOrNull(a.endDate),
          returnable = returnable
        )
      } else {
        Loaned.LoanedNotDownloaded(
          id = book.id,
          loanExpiryDate = this.someOrNull(a.endDate),
          returnable = returnable
        )
      }
    }

    private fun onIsHoldable(book: Book): BookStatus {
      return Holdable(book.id)
    }

    private fun onIsHeldNotReady(
      a: OPDSAvailabilityHeld,
      book: Book
    ): BookStatus {
      return Held.HeldInQueue(
        id = book.id,
        queuePosition = this.someOrNull(a.position),
        startDate = this.someOrNull(a.startDate),
        endDate = this.someOrNull(a.endDate),
        isRevocable = a.revoke.isSome
      )
    }

    private fun onIsHeldReady(
      a: OPDSAvailabilityHeldReady,
      book: Book
    ): BookStatus {
      return Held.HeldReady(
        id = book.id,
        endDate = this.someOrNull(a.endDate),
        isRevocable = a.revoke.isSome
      )
    }

    private fun isDRMReturnable(book: Book): Boolean {
      val format = book.findFormat(BookFormat.BookFormatEPUB::class.java)
      return format?.let {
        /*
         * XXX: I have no idea if this is correct. Does LCP have a means to "return" loans
         * outside of the Circulation Manager?
         */

        when (val info = it.drmInformation) {
          is BookDRMInformation.ACS ->
            info.acsmFile != null
          is BookDRMInformation.LCP ->
            true
          is BookDRMInformation.AXIS ->
            false
          BookDRMInformation.None ->
            false
        }
      } ?: false
    }

    private fun <T> someOrNull(x: OptionType<T>): T? {
      return if (x is Some<T>) {
        x.get()
      } else {
        null
      }
    }
  }
}
