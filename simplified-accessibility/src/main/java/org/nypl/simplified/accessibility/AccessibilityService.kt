package org.nypl.simplified.accessibility

import android.content.Context
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory

/**
 * The default accessibility service.
 */

class AccessibilityService private constructor(
  private val bookRegistry: BookRegistryReadableType,
  private val strings: AccessibilityStringsType,
  private val events: AccessibilityEventsType,
  private val uiThread: UIThreadServiceType,
) : AccessibilityServiceType {

  private val logger =
    LoggerFactory.getLogger(AccessibilityService::class.java)

  companion object {
    fun create(
      context: Context,
      bookRegistry: BookRegistryReadableType,
      uiThread: UIThreadServiceType,
      strings: AccessibilityStringsType = AccessibilityStrings(context.resources),
      events: AccessibilityEventsType = AccessibilityEvents(context)
    ): AccessibilityServiceType {
      return AccessibilityService(
        bookRegistry = bookRegistry,
        strings = strings,
        events = events,
        uiThread = uiThread
      )
    }
  }

  override val spokenFeedbackEnabled: Boolean
    get() = this.events.spokenFeedbackEnabled

  @Volatile
  private var lifecycleOwner: LifecycleOwner? = null
  private val subscriptions = CompositeDisposable()

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  override fun onViewAvailable(owner: LifecycleOwner) {
    this.lifecycleOwner = owner
    this.logger.debug("subscribing to book registry")
    this.subscriptions.add(this.bookRegistry.bookEvents().subscribe(this::onBookEvent))
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  override fun onViewUnavailable(owner: LifecycleOwner) {
    this.lifecycleOwner = owner
    this.logger.debug("unsubscribing from book registry")
    this.subscriptions.clear()
  }

  private fun speak(message: String) {
    this.uiThread.runOnUIThread {
      val owner = this.lifecycleOwner ?: return@runOnUIThread
      if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        this.speakUI(message)
      }
    }
  }

  @UiThread
  private fun speakUI(message: String) {
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
