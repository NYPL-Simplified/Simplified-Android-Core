package org.nypl.simplified.tests.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.ProcedureType
import junit.framework.Assert
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.*
import org.nypl.simplified.notifications.NotificationResourcesType
import org.nypl.simplified.notifications.NotificationsService
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.observable.ObservableType
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

    private lateinit var context: Context
    private lateinit var profileEvents: ObservableType<ProfileEvent>
    private lateinit var bookRegistry: BookRegistryType
    private lateinit var notificationsService: NotificationsService
    private lateinit var threadFactory: ThreadFactory
    private lateinit var notificationResources: NotificationResources
    private lateinit var mockNotificationManager: NotificationManager
    private var notificationCounter = 0
    private lateinit var bookWithStatusHeld: BookWithStatus
    private lateinit var bookWithStatusHeldReady: BookWithStatus

    @Before
    fun setUp() {
        this.bookRegistry = BookRegistry.create()

        val acquisition =
                OPDSAcquisition(
                        OPDSAcquisition.Relation.ACQUISITION_BORROW,
                        URI.create("http://www.example.com/0.feed"),
                        Option.some("application/vnd.adobe.adept+xml"),
                        listOf())

        val opdsEntryBuilder =
                OPDSAcquisitionFeedEntry.newBuilder(
                        "a",
                        "Title",
                        DateTime.now(),
                        OPDSAvailabilityLoanable.get())
        opdsEntryBuilder.addAcquisition(acquisition)

        val opdsEntry =
                opdsEntryBuilder.build()

        val bookId =
                BookID.create("a")

        val book =
                Book(
                        id = bookId,
                        account = AccountID.generate(),
                        cover = null,
                        thumbnail = null,
                        entry = opdsEntry,
                        formats = listOf())

        val bookStatusHeld = Mockito.mock(BookStatusHeld::class.java)
        Mockito.`when`(bookStatusHeld.id).thenReturn(bookId)

        val bookStatusHeldReady = Mockito.mock(BookStatusHeldReady::class.java)
        Mockito.`when`(bookStatusHeldReady.id).thenReturn(bookId)

        // Populate book registry
        bookWithStatusHeld = BookWithStatus.create(
                book,
                bookStatusHeld
        )

        bookWithStatusHeldReady = BookWithStatus.create(
                book,
                bookStatusHeldReady
        )

        bookRegistry.update(bookWithStatusHeld)

        this.profileEvents = Observable.create<ProfileEvent>()
        this.context = Mockito.mock(Context::class.java)
        this.threadFactory = ThreadFactory { Thread(it) }
        this.notificationResources = NotificationResources()

        notificationCounter = 0

        this.mockNotificationManager = Mockito.mock(NotificationManager::class.java)
        val mockNotification = Mockito.mock(Notification::class.java)
        Mockito.`when`(mockNotificationManager.notify(0, mockNotification))
                .then {
                    notificationCounter++
                }

        // Mock NotificationManager
        Mockito.`when`(context.getSystemService(NOTIFICATION_SERVICE))
                .thenReturn(mockNotificationManager)

        this.notificationsService = NotificationsService(context, threadFactory, profileEvents, bookRegistry, notificationResources)
    }

    class NotificationResources : NotificationResourcesType {
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
        Assert.assertEquals(context, notificationsService.context)

        // No book subscription on initialization
        Assert.assertNull(notificationsService.bookRegistrySubscription)
    }

    @Test(timeout = 3_000L)
    fun testProfileEventSubscriptionAndUnsubscription() {

        /*
         * Create a countdown latch that only accepts one count down.
         */

        val subscriptionLatch = CountDownLatch(1)
        val unsubscriptionLatch = CountDownLatch(1)

        /*
         * Create a fake observable that counts down the above latch each time someone
         * subscribes to it.
         */

        val mockSubscription = object : ObservableSubscriptionType<BookStatusEvent> {
            override fun unsubscribe() {
                unsubscriptionLatch.countDown()
            }
        }

        val mockObservable = object : ObservableReadableType<BookStatusEvent> {
            override fun subscribe(receiver: ProcedureType<BookStatusEvent>?): ObservableSubscriptionType<BookStatusEvent> {
                subscriptionLatch.countDown()
                return mockSubscription
            }

            override fun count(): Int {
                return 0
            }
        }

        val mockRegistry =
                Mockito.mock(BookRegistryType::class.java)

        Mockito.`when`(mockRegistry.bookEvents())
                .thenReturn(mockObservable)

        this.notificationsService =
                NotificationsService(context, this.threadFactory, profileEvents, mockRegistry, this.notificationResources)

        this.profileEvents.send(ProfileSelection.ProfileSelectionCompleted(ProfileID(UUID.randomUUID())))

        /*
         * Wait for the subscription latch to count down.
         * If we don't hit the await before we timeout, the subscription wasn't received.
         */

        subscriptionLatch.await()

        /**
         * Test that we can unsubscribe.
         */

        this.profileEvents.send(ProfileSelection.ProfileSelectionInProgress(ProfileID(UUID.randomUUID())))

        unsubscriptionLatch.await()
    }

    @Test
    fun testNoNotificationOnHeldReadyToHeldReadyStatus() {
        // If the book in the registry is already HeldReady we don't need to show the notification
        Assert.fail()
    }

    @Test
    fun testShowNotificationOnHeldReadyStatusChange() {
        // Show notification if new status is HeldReady and cache is Held

        /**
         * Register profile event completed so we subscribe to the events from books
         */
        this.profileEvents.send(ProfileSelection.ProfileSelectionCompleted(ProfileID(UUID.randomUUID())))

        Thread.sleep(5000)

        /**
         * Send a book update that should trigger the notification
         */

        bookRegistry.update(bookWithStatusHeldReady)


        Thread.sleep(5000)
        Assert.assertEquals(1, notificationCounter)
    }
}