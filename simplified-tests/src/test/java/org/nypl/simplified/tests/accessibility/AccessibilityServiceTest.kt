package org.nypl.simplified.tests.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.nypl.simplified.accessibility.AccessibilityService
import org.nypl.simplified.accessibility.AccessibilityServiceType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedDownloaded
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.mocking.MockAccessibilityEvents
import org.nypl.simplified.tests.mocking.MockAccessibilityStrings
import org.nypl.simplified.tests.mocking.MockLifecycle
import org.nypl.simplified.tests.mocking.MockUIThreadService
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import java.util.UUID

class AccessibilityServiceTest {

  private lateinit var book0: Book
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var context: Context
  private lateinit var lifecycle: MockLifecycle
  private lateinit var lifecycleOwner: LifecycleOwner
  private lateinit var mockAccessService: AccessibilityManager
  private lateinit var service: AccessibilityServiceType
  private lateinit var strings: MockAccessibilityStrings
  private lateinit var events: MockAccessibilityEvents
  private lateinit var uiThread: UIThreadServiceType

  private val failure =
    TaskResult.fail<Unit>("x", "x", "x") as TaskResult.Failure<Unit>

  @BeforeEach
  fun setup() {
    this.context =
      Mockito.mock(Context::class.java)
    this.uiThread =
      MockUIThreadService()
    this.strings =
      MockAccessibilityStrings()
    this.events =
      MockAccessibilityEvents()
    this.bookRegistry =
      BookRegistry.create()
    this.mockAccessService =
      Mockito.mock(AccessibilityManager::class.java)
    this.lifecycleOwner =
      Mockito.mock(LifecycleOwner::class.java)
    this.lifecycle =
      MockLifecycle()

    Mockito.`when`(this.lifecycleOwner.lifecycle)
      .thenReturn(this.lifecycle)
    Mockito.`when`(this.context.getSystemService(Context.ACCESSIBILITY_SERVICE))
      .thenReturn(this.mockAccessService)
    this.turnOnScreenReader()

    this.service =
      AccessibilityService.create(
        context = this.context,
        bookRegistry = this.bookRegistry,
        uiThread = this.uiThread,
        strings = this.strings,
        events = this.events
      )

    this.book0 =
      Book(
        BookIDs.newFromText("x"),
        AccountID(UUID.randomUUID()),
        null,
        null,
        OPDSAcquisitionFeedEntry.newBuilder(
          "hello", "Book", DateTime.now(), OPDSAvailabilityLoanable.get()
        )
          .build(),
        listOf()
      )
  }

  private fun turnOnScreenReader() {
    Mockito.`when`(this.mockAccessService.getEnabledAccessibilityServiceList(FEEDBACK_SPOKEN))
      .thenReturn(listOf(AccessibilityServiceInfo()))
  }

  private fun turnOffScreenReader() {
    Mockito.`when`(this.mockAccessService.getEnabledAccessibilityServiceList(FEEDBACK_SPOKEN))
      .thenReturn(listOf())
  }

  /**
   * If a book suddenly becomes downloaded, then nothing happens if it would cause
   * a lifecycle issue.
   */

  @Test
  fun testBookDownloadedButNotAttached() {
    assertEquals(0, this.events.events.size)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = LoanedDownloaded(this.book0.id, null, false)
      )
    )

    assertEquals(0, this.events.events.size)
  }

  /**
   * If a book becomes downloaded, but wasn't previously in any particular state, then no
   * event will be published.
   */

  @Test
  fun testBookDownloadedNoPrior() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = LoanedDownloaded(this.book0.id, null, false)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals(0, this.events.events.size)
  }

  /**
   * If a book suddenly becomes downloaded, then a toast is shown with the right message.
   */

  @Test
  fun testBookDownloaded() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.Downloading(this.book0.id, 0, 100, "OK")
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = LoanedDownloaded(this.book0.id, null, false)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookHasDownloaded Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }

  /**
   * Book downloads show the right events.
   */

  @Test
  fun testBookDownloadingDownloaded() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.RequestingDownload(this.book0.id)
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.Downloading(this.book0.id, 0, 100, "OK")
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.Downloading(this.book0.id, 0, 100, "OK")
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = LoanedDownloaded(this.book0.id, null, false)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookIsDownloading Book", this.events.events.removeAt(0))
    assertEquals("bookHasDownloaded Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }

  /**
   * Failed loans show the right events.
   */

  @Test
  fun testBookFailedLoan() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.RequestingDownload(this.book0.id)
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.FailedLoan(this.book0.id, failure)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookFailedLoan Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }

  /**
   * Failed downloads show the right events.
   */

  @Test
  fun testBookFailedDownload() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.RequestingDownload(this.book0.id)
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.FailedDownload(this.book0.id, failure)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookFailedDownload Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }

  /**
   * Failed returns show the right events.
   */

  @Test
  fun testBookFailedReturn() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.RequestingRevoke(this.book0.id)
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.FailedRevoke(this.book0.id, failure)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookFailedReturn Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }

  /**
   * Placing a book on hold shows the right events.
   */

  @Test
  fun testBookOnHold() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.RequestingLoan(this.book0.id, "x")
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.Held.HeldInQueue(this.book0.id, null, null, false, null)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookIsOnHold Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }

  /**
   * Returning a book shows the right events.
   */

  @Test
  fun testBookReturn() {
    assertEquals(0, this.events.events.size)

    this.lifecycle.state = Lifecycle.State.STARTED
    this.service.onViewAvailable(this.lifecycleOwner)

    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.RequestingRevoke(this.book0.id)
      )
    )
    this.bookRegistry.update(
      BookWithStatus(
        book = this.book0,
        status = BookStatus.Loanable(this.book0.id)
      )
    )

    this.service.onViewUnavailable(this.lifecycleOwner)

    assertEquals("bookReturned Book", this.events.events.removeAt(0))
    assertEquals(0, this.events.events.size)
  }
}
