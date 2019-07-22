package org.nypl.simplified.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.R
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileSelected
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService

class NotificationsService(
        val context: Context,
        val profileEvents: ObservableReadableType<ProfileEvent>,
        val bookRegistry: BookRegistryReadableType) {


    companion object {
        const val NOTIFICATION_PRIMARY_CHANNEL_ID = "simplified_notification_channel_primary"
    }

    private val logger = LoggerFactory.getLogger(NotificationsService::class.java)

    private val executor: ExecutorService = NamedThreadPools.namedThreadPool(1, "notifications", 19)

    val profileSubscription =
            profileEvents.subscribe(this::onProfileEvent)
    val bookRegistrySubscription =
            bookRegistry.bookEvents().subscribe(this::onBookEvent)

    /*
    When changing from SimplyE to NYPL, the ProfileAccountSelectSucceededEvent is firing
     */
    private fun onProfileEvent(event: ProfileEvent) {
        logger.debug("NotificationsService::onProfileEvent $event")
        executor.execute {
            if (event is ProfileSelected ||
                    event is ProfileAccountSelectEvent.ProfileAccountSelectSucceeded) {
                publishNotification("onProfileEvent", event.toString())
                getBookStatuses()
            }
        }
    }

    /*

     */
    private fun onBookEvent(event: BookEvent) {
        logger.debug("NotificationsService:: onBookEvent $event")
        executor.execute {
            if (event is BookStatusEvent) {
                /*
                   BookStatusEvent is only giving me BOOK_CHANGED and BOOK_REMOVED.
                   While a book Download goes on, this fires repeatedly as the download
                   progress get updated.
                 */

                publishNotification("onBookEvent", event.type().toString())
                // getBookStatuses()
            }
        }
    }

    private fun getBookStatuses() {
        val statusesBefore: Map<BookID, BookWithStatus> = mapOf()
        val statusesNow = getBookStatusesFromRegistry()
        for (book in statusesBefore) {
            var bookId = book.key
            val statusBefore = statusesBefore[bookId]
            val statusNow = statusesNow[bookId]

            // Do any comparison on statuses here
            if (statusChangedSufficiently(statusBefore, statusNow)) {
//                publishNotification(...)
            }
        }

        //saveStatuses(statusesNow)
    }

    private fun statusChangedSufficiently(statusBefore: BookWithStatus?, statusNow: BookWithStatus?): Boolean {
        return false
    }

    private fun getBookStatusesFromRegistry(): Map<BookID, BookWithStatus> {
        var books = bookRegistry.books()
        var statuses: MutableMap<BookID, BookWithStatus> = mutableMapOf()

        for (book in books) {
            val bookId = book.key
            statuses[bookId] = (bookRegistry.book(bookId) as Some<BookWithStatus>).get()
        }

        return statuses.toMap()
    }

    private fun publishNotification(notificationTitle: String, notificationContent: String) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager, "Channel Name", "Channel Description")

        var builder = NotificationCompat.Builder(context, NOTIFICATION_PRIMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.audiobook_icon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(0, builder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager,
                                          channelName: String,
                                          channelDescription: String) {
        logger.debug("createNotificationChannel")

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = channelName
            val descriptionText = channelDescription
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_PRIMARY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }
}
