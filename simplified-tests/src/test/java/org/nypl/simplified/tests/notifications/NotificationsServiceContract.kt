package org.nypl.simplified.tests.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import io.reactivex.subjects.PublishSubject
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.notifications.NotificationResourcesType
import org.nypl.simplified.notifications.NotificationsService
import org.nypl.simplified.notifications.NotificationsWrapper
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.tests.R
import org.nypl.simplified.tests.books.book_database.BookDatabaseContract
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadFactory

abstract class NotificationsServiceContract {
  private val logger =
    LoggerFactory.getLogger(BookDatabaseContract::class.java)

  private lateinit var mockContext: Context
  private lateinit var profileEvents: PublishSubject<ProfileEvent>
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var notificationsService: NotificationsService
  private lateinit var threadFactory: ThreadFactory
  private lateinit var notificationResources: NotificationResources
  private lateinit var mockNotificationManager: NotificationManager
  private lateinit var mockNotificationsWrapper: NotificationsWrapper

  /**
   * Books we'll use to update the registry and trigger events
   */

  private lateinit var bookWithStatusHeld: BookWithStatus
  private lateinit var bookWithStatusHeldReady: BookWithStatus
  private lateinit var bookWithStatusHeldReady2: BookWithStatus

  /**
   * Counter we'll increment when mocking notification calls.
   */

  private var notificationCounter = 0

  @Before
  fun setUp() {
    logger.debug("setup")
    mockContext = Mockito.mock(Context::class.java)

    threadFactory = ThreadFactory { Thread(it) }
    bookRegistry = BookRegistry.create()
    profileEvents = PublishSubject.create<ProfileEvent>()
    notificationResources = NotificationResources()

    /**
     * Build books for testing
     */

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        MIMEParser.parseRaisingException("application/vnd.adobe.adept+xml"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntryBuilder2 =
      OPDSAcquisitionFeedEntry.newBuilder(
        "b",
        "Title B",
        DateTime.now(),
        OPDSAvailabilityLoanable.get()
      )
    opdsEntryBuilder2.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    val opdsEntry2 =
      opdsEntryBuilder2.build()

    val bookId =
      BookID.create("a")

    val book =
      Book(
        id = bookId,
        account = AccountID.generate(),
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    val bookId2 =
      BookID.create("b")

    val book2 =
      Book(
        id = bookId2,
        account = AccountID.generate(),
        cover = null,
        thumbnail = null,
        entry = opdsEntry2,
        formats = listOf()
      )

    val bookStatusHeld = Mockito.mock(BookStatus.Held.HeldInQueue::class.java)
    Mockito.`when`(bookStatusHeld.id).thenReturn(bookId)

    val bookStatusHeldReady = Mockito.mock(BookStatus.Held.HeldReady::class.java)
    Mockito.`when`(bookStatusHeldReady.id).thenReturn(bookId)

    val bookStatusHeldReady2 = Mockito.mock(BookStatus.Held.HeldReady::class.java)
    Mockito.`when`(bookStatusHeldReady.id).thenReturn(bookId2)

    // Populate book registry
    bookWithStatusHeld = BookWithStatus(
      book,
      bookStatusHeld
    )

    bookWithStatusHeldReady = BookWithStatus(
      book,
      bookStatusHeldReady
    )

    bookWithStatusHeldReady2 = BookWithStatus(
      book2,
      bookStatusHeldReady2
    )

    /**
     * Populate registry with initial record(s)
     */

    bookRegistry.update(bookWithStatusHeld)

    /**
     * Reset notification counter for each test
     */

    notificationCounter = 0

    /**
     * Setup notification mocks
     */

    mockNotificationManager = Mockito.mock(NotificationManager::class.java)
    mockNotificationsWrapper = Mockito.mock(NotificationsWrapper::class.java)
    Mockito.`when`(mockNotificationsWrapper.postDefaultNotification(notificationResources))
      .then {
        notificationCounter++
      }

    Mockito.`when`(mockContext.getSystemService(NOTIFICATION_SERVICE))
      .thenReturn(mockNotificationManager)

    /**
     * Initialize [NotificationsService]
     */

    notificationsService = NotificationsService(mockContext, threadFactory, profileEvents, bookRegistry, mockNotificationsWrapper, notificationResources)
  }

  class NotificationResources : NotificationResourcesType {
    override val notificationChannelName: String
      get() = "notification channel name"
    override val notificationChannelDescription: String
      get() = "notification channel description"
    override val intentClass: Class<*>
      get() = String::class.java
    override val titleReadyNotificationContent: String
      get() = "notification content"
    override val titleReadyNotificationTitle: String
      get() = "notification title"
    override val smallIcon: Int
      get() = R.drawable.simplified_button
  }

  @Test
  fun testInitializeNotificationsService() {
    // Instance created and properties assigned
    Assert.assertNotNull(notificationsService)

    Assert.assertEquals(profileEvents, notificationsService.profileEvents)
    Assert.assertEquals(bookRegistry, notificationsService.bookRegistry)
    Assert.assertEquals(mockContext, notificationsService.context)

    // No book subscription on initialization
    Assert.assertNull(notificationsService.bookRegistrySubscription)
  }

  @Test(timeout = 3_000L)
  fun testProfileEventSubscriptionAndUnsubscription() {
    /**
     * Create a countdown latch that only accepts one count down.
     */

    val subscriptionLatch = CountDownLatch(1)
    val unsubscriptionLatch = CountDownLatch(1)

    /**
     * Create a fake observable that counts down the above latch each time someone
     * subscribes to it.
     */

    val mockObservable =
      PublishSubject.create<BookStatusEvent>()
        .doOnSubscribe { subscriptionLatch.countDown() }
        .doOnDispose { unsubscriptionLatch.countDown() }

    val mockRegistry =
      Mockito.mock(BookRegistryType::class.java)

    Mockito.`when`(mockRegistry.bookEvents())
      .thenReturn(mockObservable)

    /**
     * Re-initialize the [NotificationsService] with our mockRegistry for testing
     */

    notificationsService =
      NotificationsService(mockContext, threadFactory, profileEvents, mockRegistry, mockNotificationsWrapper, notificationResources)

    profileEvents.onNext(ProfileSelection.ProfileSelectionCompleted(ProfileID(UUID.randomUUID())))

    /**
     * Wait for the subscription latch to count down.
     * If we don't hit the await before we timeout, the subscription wasn't received.
     */

    subscriptionLatch.await()

    /**
     * Test that we can unsubscribe.
     */

    profileEvents.onNext(ProfileSelection.ProfileSelectionInProgress(ProfileID(UUID.randomUUID())))

    unsubscriptionLatch.await()
  }

  @Test
  fun testNoNotificationOnHeldReadyToHeldReadyStatus() {
    // If the book in the registry is already HeldReady we don't need to show the notification

    /**
     * Register profile event completed so we subscribe to the events from books
     */
    profileEvents.onNext(ProfileSelection.ProfileSelectionCompleted(ProfileID(UUID.randomUUID())))

    Thread.sleep(300)

    /**
     * Send a book update that should not trigger the notification
     */

    bookRegistry.update(bookWithStatusHeld)

    Thread.sleep(300)
    Assert.assertEquals(0, notificationCounter)
  }

  @Test
  fun testShowNotificationOnHeldReadyStatusChange() {
    // Show notification if new status is HeldReady and cache is Held

    /**
     * Register profile event completed so we subscribe to the events from books
     */
    profileEvents.onNext(ProfileSelection.ProfileSelectionCompleted(ProfileID(UUID.randomUUID())))

    Thread.sleep(300)

    /**
     * Send a book update that should trigger the notification
     */

    bookRegistry.update(bookWithStatusHeldReady)

    Thread.sleep(300)
    Assert.assertEquals(1, notificationCounter)
  }

  @Test
  fun testNoNotificationOnHeldReadyStatusChangeForOtherBook() {
    // Do not show a notification if a book in HeldReady status was not already in the registry

    /**
     * Register profile event completed so we subscribe to the events from books
     */
    profileEvents.onNext(ProfileSelection.ProfileSelectionCompleted(ProfileID(UUID.randomUUID())))

    Thread.sleep(300)

    /**
     * Send a book update for HeldReady that was not in directory
     */

    bookRegistry.update(bookWithStatusHeldReady2)

    Thread.sleep(300)
    Assert.assertEquals(0, notificationCounter)
  }
}
