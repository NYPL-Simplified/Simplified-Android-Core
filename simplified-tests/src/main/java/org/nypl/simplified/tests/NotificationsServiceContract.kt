package org.nypl.simplified.tests

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.notifications.NotificationResourcesType
import org.nypl.simplified.notifications.NotificationsService
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.tests.books.book_database.BookDatabaseContract
import org.slf4j.LoggerFactory

abstract class NotificationsServiceContract {
    private val logger =
            LoggerFactory.getLogger(BookDatabaseContract::class.java)

    private lateinit var profileEvents: ObservableType<ProfileEvent>
    private lateinit var bookRegistry: BookRegistryType

    @Before
    fun setUp() {
        this.bookRegistry = BookRegistry.create()
        this.profileEvents = Observable.create<ProfileEvent>()
    }

    @Test
    fun showNotificationOnHeldReady() {
        val context = Mockito.mock(Context::class.java)

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

        val notificationsService = NotificationsService(context, profileEvents, bookRegistry, notificationResourcesType)
    }
}