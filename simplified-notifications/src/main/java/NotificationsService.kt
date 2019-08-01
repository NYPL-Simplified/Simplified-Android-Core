package org.nypl.simplified.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.io7m.jfunctional.Some
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookStatusHeld
import org.nypl.simplified.books.book_registry.BookStatusHeldReady
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory

class NotificationsService(
        val context: Context,
        val threadFactory: ThreadFactory,
        val profileEvents: ObservableReadableType<ProfileEvent>,
        val bookRegistry: BookRegistryReadableType,
        val notificationResourcesType: NotificationResourcesType) {


    companion object {
        const val NOTIFICATION_PRIMARY_CHANNEL_ID = "simplified_notification_channel_primary"
    }

    private val logger = LoggerFactory.getLogger(NotificationsService::class.java)

    private val executor: ExecutorService = NamedThreadPools.namedThreadPoolOf(1, this.threadFactory)

    val profileSubscription: ObservableSubscriptionType<ProfileEvent>? =
            profileEvents.subscribe(this::onProfileEvent)

    var bookRegistrySubscription: ObservableSubscriptionType<BookStatusEvent>? = null

    var registryCache: Map<BookID, BookWithStatus> = mapOf()


    /**
     * Method for handling [ProfileEvent]s the service is subscribed to.
     */
    private fun onProfileEvent(event: ProfileEvent) {
        logger.debug("NotificationsService::onProfileEvent $event")
        executor.execute {
            if (event is ProfileSelection.ProfileSelectionInProgress) {
                onProfileSelectionInProgress()
            } else if (event is ProfileSelection.ProfileSelectionCompleted) {
                onProfileSelectionCompleted()
            }
        }
    }

    /**
     * Defines logic for handling [ProfileEvent] [ProfileSelection.ProfileSelectionInProgress].
     * Unsubscribes from [BookEvent]s and clears the cached bookRegistry.
     * Once the profile selection is completed, [onProfileSelectionCompleted] will
     * handle resubscribing for changes.
     */
    private fun onProfileSelectionInProgress() {
        logger.debug("NotificationsService::onProfileSelectionInProgress")
        unsubscribeFromBookEvents()
        clearBookRegistryCache()
    }

    /**
     * Defines logic for handling [ProfileEvent] [ProfileSelection.ProfileSelectionCompleted].
     * Caches the bookRegistry and subscribes to [BookEvent]s.
     * When the [ProfileEvent] [ProfileSelection.ProfileSelectionCompleted] fires,
     * the registry will represent what is currently on disc.
     */
    private fun onProfileSelectionCompleted() {
        logger.debug("NotificationsService::onProfileSelectionCompleted")
        cacheBookRegistry()
        subscribeToBookEvents()
    }

    /**
     * Method for handling [BookEvent]s the service is subscribed to.
     */
    private fun onBookEvent(event: BookEvent) {
        logger.debug("NotificationsService::onBookEvent $event")
        executor.execute {
            if (event is BookStatusEvent) {
                /*
                   When a BookStatusEvent with Type BOOK_CHANGED, we want to compare
                   the current registry's status with the cached version.
                 */
                if (event.type() == BookStatusEvent.Type.BOOK_CHANGED) {
                    val bookStatus = (bookRegistry.book(event.book()) as Some<BookWithStatus>).get()
                    compareToCache(bookStatus)
                }
            }
        }
    }

    private fun compareToCache(bookStatus: BookWithStatus?) {
        logger.debug("NotificationsService::compareToCache ${bookStatus?.status()}")
        // TODO: Validate this behavior.
        var cachedBookStatus = registryCache[bookStatus?.book()?.id]
        if (statusChangedSufficiently(bookStatus, cachedBookStatus)) {
            publishNotification(notificationResourcesType.titleReadyNotificationTitle,
                    notificationResourcesType.titleReadyNotificationContent)
        }
    }

    /**
     * Caches the BookRegistry.
     * This is used to check against [BookStatusEvent]s if a new book
     * is in a status we want to take action on (i.e. show notification)
     */
    private fun cacheBookRegistry() {
        logger.debug("In NotificationsService::cacheBookRegistry")
        registryCache = getBookStatusesFromRegistry()
        logger.debug("registryCache has been populated with ${registryCache.size} books")
    }

    /**
     * Resets the cached BookRegistry.
     */
    private fun clearBookRegistryCache() {
        logger.debug("In NotificationsService::clearBookRegistryCache")
        registryCache = mapOf()
    }

    /**
     * Subscribes [NotificationsService] to receive [BookStatusEvent]s
     */
    private fun subscribeToBookEvents() {
        logger.debug("NotificationsService::subscribeToBookEvents")
        bookRegistrySubscription =
                bookRegistry.bookEvents().subscribe(this::onBookEvent)
    }

    /**
     * Unsubscribes [NotificationsService] from [BookStatusEvent]s
     */
    private fun unsubscribeFromBookEvents() {
        logger.debug("NotificationsService::unsubscribeFromBookEvents")
        bookRegistrySubscription?.unsubscribe()
    }

    private fun statusChangedSufficiently(statusBefore: BookWithStatus?, statusNow: BookWithStatus?): Boolean {
        // Compare statusBefore and statusNow, only return true if statusNow is actually [BookStatusHeldReady]
        logger.debug("NotificationsService::statusChangedSufficiently comparing ${statusBefore?.status()} to ${statusNow?.status()}")
        if (statusBefore != null && statusNow != null) {
            return statusBefore.status() is BookStatusHeld &&
                    statusNow.status() is BookStatusHeldReady
        }

        // No status change met criteria
        return false
    }

    /**
     *
     */
    private fun getBookStatusesFromRegistry(): Map<BookID, BookWithStatus> {
        logger.debug("NotificationsService::getBookStatusesFromRegistry")
        logger.debug("bookRegistry has ${bookRegistry.books().size} books")
        return bookRegistry.books().toMap()
    }

    private fun publishNotification(notificationTitle: String, notificationContent: String) {
        logger.debug("NotificationsService::publishNotification " +
                "with title $notificationTitle and content $notificationContent")
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager, "Channel Name", "Channel Description")

        val intent = Intent(context, notificationResourcesType.intentClass)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        var builder = NotificationCompat.Builder(context, NOTIFICATION_PRIMARY_CHANNEL_ID)
                .setSmallIcon(notificationResourcesType.smallIcon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

        notificationManager.notify(0, builder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager,
                                          channelName: String,
                                          channelDescription: String) {
        logger.debug("NotificationsService::createNotificationChannel " +
                "with channel name $channelName and description $channelDescription")

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
