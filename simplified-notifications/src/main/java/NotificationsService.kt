package org.nypl.simplified.notifications

import android.content.Context
import com.io7m.jfunctional.Some
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory

class NotificationsService(
  val context: Context,
  val threadFactory: ThreadFactory,
  val profileEvents: Observable<ProfileEvent>,
  val bookRegistry: BookRegistryReadableType,
  private val notificationsWrapper: NotificationsWrapper,
  val notificationResourcesType: NotificationResourcesType
) {

  companion object {
    const val NOTIFICATION_PRIMARY_CHANNEL_ID = "simplified_notification_channel_primary"
  }

  private val logger = LoggerFactory.getLogger(NotificationsService::class.java)

  private val executor: ExecutorService = NamedThreadPools.namedThreadPoolOf(1, threadFactory)

  private val profileSubscription: Disposable? =
    profileEvents.subscribe(::onProfileEvent)

  private var registryCache: Map<BookID, BookWithStatus> = mapOf()

  // Start null until we have a profile selected event
  var bookRegistrySubscription: Disposable? = null

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
    logger.trace("NotificationsService::onBookEvent $event")
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

  /**
   * Performs a check of the new status against our [BookRegistry] cache and
   * posts a notification if it satisfies the rules for doing so.
   */
  private fun compareToCache(bookStatus: BookWithStatus?) {
    logger.trace("NotificationsService::compareToCache ${bookStatus?.status}")
    var cachedBookStatus = registryCache[bookStatus?.book?.id]
    if (statusChangedSufficiently(cachedBookStatus, bookStatus)) {
      publishNotification(
        notificationResourcesType.titleReadyNotificationTitle,
        notificationResourcesType.titleReadyNotificationContent
      )
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
      bookRegistry.bookEvents().subscribe(::onBookEvent)
  }

  /**
   * Unsubscribes [NotificationsService] from [BookStatusEvent]s
   */
  private fun unsubscribeFromBookEvents() {
    logger.debug("NotificationsService::unsubscribeFromBookEvents")
    bookRegistrySubscription?.dispose()
  }

  /**
   * Checks that the new status satisfies the rules for prompting us to show a notification.
   */
  private fun statusChangedSufficiently(statusBefore: BookWithStatus?, statusNow: BookWithStatus?): Boolean {
    // Compare statusBefore and statusNow, only return true if statusNow is actually [BookStatusHeldReady]
    logger.debug("NotificationsService::statusChangedSufficiently comparing ${statusBefore?.status} to ${statusNow?.status}")
    if (statusBefore != null && statusNow != null) {
      return statusBefore.status is BookStatus.Held.HeldInQueue &&
        statusNow.status is BookStatus.Held.HeldReady
    }

    // No status change met criteria
    return false
  }

  /**
   * Returns a map of the books in the [BookRegistry]
   */
  private fun getBookStatusesFromRegistry(): Map<BookID, BookWithStatus> {
    logger.debug("NotificationsService::getBookStatusesFromRegistry")
    logger.debug("bookRegistry has ${bookRegistry.books().size} books")
    return bookRegistry.books().toMap()
  }

  /**
   * Calls to the [NotificationsWrapper] to post a notification.
   */
  private fun publishNotification(notificationTitle: String, notificationContent: String) {
    logger.debug(
      "NotificationsService::publishNotification " +
        "with title $notificationTitle and content $notificationContent"
    )
    notificationsWrapper.postDefaultNotification(notificationResourcesType)
  }
}
