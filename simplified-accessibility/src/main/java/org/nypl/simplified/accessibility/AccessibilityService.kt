package org.nypl.simplified.accessibility

import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.slf4j.LoggerFactory

/**
 * The default accessibility service.
 */

class AccessibilityService private constructor(
  private val bookRegistry: BookRegistryReadableType,
  private val strings: AccessibilityStringsType,
  private val events: AccessibilityEventsType,
) : AccessibilityServiceType {

  companion object {
    fun create(
      context: Context,
      bookRegistry: BookRegistryReadableType,
      strings: AccessibilityStringsType = AccessibilityStrings(context.resources),
      events: AccessibilityEventsType = AccessibilityEvents(context)
    ): AccessibilityServiceType {
      return AccessibilityService(
        bookRegistry = bookRegistry,
        strings = strings,
        events = events
      )
    }
  }

  private val logger =
    LoggerFactory.getLogger(AccessibilityService::class.java)

  private val subscriptions =
    CompositeDisposable(
      bookRegistry.bookEvents().subscribe(this::onBookEvent)
    )

  override val spokenFeedbackEnabled: Boolean
    get() = this.events.spokenFeedbackEnabled

  private fun speak(message: String) {
    this.events.show(message)
  }

  private fun <C : BookStatus> previousStatusIsNot(
    event: BookStatusEvent,
    statusClass: Class<C>
  ): Boolean {
    val statusPrevious = event.statusPrevious ?: return false
    return statusPrevious::class.java != statusClass
  }

  private fun onBookEvent(event: BookStatusEvent) {
    val book = this.bookRegistry.bookOrNull(event.bookId)?.book ?: return

    return when (event.statusNow) {
      is BookStatus.Loaned.LoanedDownloaded ->
        if (this.previousStatusIsNot(event, BookStatus.Loaned.LoanedDownloaded::class.java)) {
          this.speak(this.strings.bookHasDownloaded(book.entry.title))
        } else {
          // Nothing to do
        }

      is BookStatus.Downloading ->
        if (this.previousStatusIsNot(event, BookStatus.Downloading::class.java)) {
          this.speak(this.strings.bookIsDownloading(book.entry.title))
        } else {
          // Nothing to do
        }

      is BookStatus.Held.HeldInQueue ->
        if (this.previousStatusIsNot(event, BookStatus.Held.HeldInQueue::class.java)) {
          this.speak(this.strings.bookIsOnHold(book.entry.title))
        } else {
          // Nothing to do
        }

      is BookStatus.Holdable,
      is BookStatus.Loanable -> {
        val notHoldable = this.previousStatusIsNot(event, BookStatus.Holdable::class.java)
        val notLoanable = this.previousStatusIsNot(event, BookStatus.Loanable::class.java)
        if (notHoldable && notLoanable) {
          this.speak(this.strings.bookReturned(book.entry.title))
        } else {
          // Nothing to do
        }
      }

      is BookStatus.FailedRevoke ->
        if (this.previousStatusIsNot(event, BookStatus.FailedRevoke::class.java)) {
          this.speak(this.strings.bookFailedReturn(book.entry.title))
        } else {
          // Nothing to do
        }

      is BookStatus.FailedLoan ->
        if (this.previousStatusIsNot(event, BookStatus.FailedLoan::class.java)) {
          this.speak(this.strings.bookFailedLoan(book.entry.title))
        } else {
          // Nothing to do
        }

      is BookStatus.FailedDownload ->
        if (this.previousStatusIsNot(event, BookStatus.FailedDownload::class.java)) {
          this.speak(this.strings.bookFailedDownload(book.entry.title))
        } else {
          // Nothing to do
        }

      is BookStatus.Held.HeldReady,
      is BookStatus.Loaned.LoanedNotDownloaded,
      is BookStatus.RequestingRevoke,
      is BookStatus.RequestingLoan,
      is BookStatus.RequestingDownload,
      is BookStatus.DownloadWaitingForExternalAuthentication,
      is BookStatus.DownloadExternalAuthenticationInProgress,
      is BookStatus.Revoked -> {
      }
      null -> {
      }
    }
  }
}
