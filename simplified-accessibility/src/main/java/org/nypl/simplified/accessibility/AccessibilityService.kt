package org.nypl.simplified.accessibility

import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.view.accessibility.AccessibilityManager
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

/**
 * The default accessibility service.
 */

class AccessibilityService private constructor(
  private val accessibilityManager: AccessibilityManager,
  private val bookRegistry: BookRegistryReadableType,
  private val strings: AccessibilityStringsType,
  private val toasts: AccessibilityToastsType,
  private val uiThread: UIThreadServiceType,
) : AccessibilityServiceType {

  companion object {
    fun create(
      context: Context,
      bookRegistry: BookRegistryReadableType,
      uiThread: UIThreadServiceType,
      strings: AccessibilityStringsType = AccessibilityStrings(context.resources),
      toasts: AccessibilityToastsType = AccessibilityToasts(context)
    ): AccessibilityServiceType {
      val accessibilityManager =
        context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
      return AccessibilityService(
        accessibilityManager = accessibilityManager,
        bookRegistry = bookRegistry,
        strings = strings,
        toasts = toasts,
        uiThread = uiThread
      )
    }
  }

  private fun isSpokenFeedbackEnabled(): Boolean {
    return this.accessibilityManager
      .getEnabledAccessibilityServiceList(FEEDBACK_SPOKEN)
      .isNotEmpty()
  }

  override val spokenFeedbackEnabled: Boolean
    get() = this.isSpokenFeedbackEnabled()

  @Volatile
  private var lifecycleOwner: LifecycleOwner? = null
  private val subscriptions = CompositeDisposable()

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  override fun onViewAvailable(owner: LifecycleOwner) {
    this.lifecycleOwner = owner
    this.subscriptions.add(this.bookRegistry.bookEvents().subscribe(this::onBookEvent))
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  override fun onViewUnavailable(owner: LifecycleOwner) {
    this.lifecycleOwner = owner
    this.subscriptions.dispose()
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
    if (this.spokenFeedbackEnabled || AccessibilityDebugging.alwaysShowToasts) {
      this.toasts.show(message)
    }
  }

  private fun onBookEvent(event: BookStatusEvent) {
    val book = this.bookRegistry.bookOrNull(event.bookId)?.book ?: return

    return when (event.statusNow) {
      is BookStatus.Loaned.LoanedDownloaded ->
        this.speak(this.strings.bookHasDownloaded(book.entry.title))

      is BookStatus.Downloading -> {
        if (!(event.statusPrevious is BookStatus.Downloading)) {
          this.speak(this.strings.bookIsDownloading(book.entry.title))
        } else {
          // Already downloading.
        }
      }

      is BookStatus.Held.HeldInQueue,
      is BookStatus.Held.HeldReady,
      is BookStatus.FailedRevoke,
      is BookStatus.FailedDownload,
      is BookStatus.FailedLoan,
      is BookStatus.Holdable,
      is BookStatus.Loanable,
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
