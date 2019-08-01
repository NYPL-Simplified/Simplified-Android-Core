package org.nypl.simplified.tests.notifications

import android.content.Context
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.notifications.NotificationResourcesType
import org.nypl.simplified.notifications.NotificationsService
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.tests.R
import org.nypl.simplified.tests.books.book_database.BookDatabaseContract
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ThreadFactory

abstract class NotificationsServiceContract {
    private val logger =
            LoggerFactory.getLogger(BookDatabaseContract::class.java)

    private lateinit var context: Context
    private lateinit var profileEvents: ObservableType<ProfileEvent>
    private lateinit var bookRegistry: BookRegistryType
    private lateinit var notificationsService: NotificationsService
    private lateinit var threadFactory: ThreadFactory

    @Before
    fun setUp() {
        this.bookRegistry = BookRegistry.create()
        this.profileEvents = Observable.create<ProfileEvent>()
        this.context = Mockito.mock(Context::class.java)
        this.threadFactory = ThreadFactory { Thread(it) }

        val notificationResourcesType = object : NotificationResourcesType {
            override val intentClass: Class<*>
                get() = String::class.java
            override val titleReadyNotificationContent: String
                get() = "notification content"
            override val titleReadyNotificationTitle: String
                get() = "notification title"
            override val smallIcon: Int
                get() = R.drawable.simplified_button
        }

        this.notificationsService = NotificationsService(context, this.threadFactory, profileEvents, bookRegistry, notificationResourcesType)
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

    @Test
    fun testOnProfileSelectionCompletedSetsBookRegistrySubscription() {
        this.profileEvents.send(ProfileAccountSelectEvent.ProfileAccountSelectSucceeded.of(AccountID(UUID.randomUUID()), AccountID(UUID.randomUUID())))
        Assert.assertNotNull(notificationsService.bookRegistrySubscription)
    }

    @Test
    fun testOnProfileSelectionCompletedSetBookRegistryCache() {
        Assert.fail()
    }

    @Test
    fun testOnProfileSelectionInProgressUnsubscribeBookEvents() {
        Assert.fail()
    }

    @Test
    fun testOnProfileSelectionInProgressClearsBookRegistryCache() {
        Assert.fail()
    }

    @Test
    fun testNoNotificationOnHeldReadyToHeldReadyStatus() {
        // If the book in the registry is already HeldReady we don't need to show the notification
        Assert.fail()
    }

    @Test
    fun testShowNotificationOnHeldReadyStatusChange() {
        // Show notification if new status is HeldReady and cache is Held
        Assert.fail()
    }
}