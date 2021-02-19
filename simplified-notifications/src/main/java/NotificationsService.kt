package org.nypl.simplified.notifications

import android.content.Context
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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

  private val logger =
    LoggerFactory.getLogger(NotificationsService::class.java)
  private val executor: ExecutorService =
    NamedThreadPools.namedThreadPoolOf(1, this.threadFactory)
  private val profileSubscription: Disposable? =
    this.profileEvents.subscribe(::onProfileEvent)

  // Start null until we have a profile selected event
  var bookRegistrySubscription: Disposable? = null

  /**
   * Method for handling [ProfileEvent]s the service is subscribed to.
   */
  private fun onProfileEvent(event: ProfileEvent) {
    this.logger.debug("NotificationsService::onProfileEvent $event")
    this.executor.execute {
      if (event is ProfileSelection.ProfileSelectionInProgress) {
        this.onProfileSelectionInProgress()
      } else if (event is ProfileSelection.ProfileSelectionCompleted) {
        this.onProfileSelectionCompleted()
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
    this.logger.debug("NotificationsService::onProfileSelectionInProgress")
    this.unsubscribeFromBookEvents()
  }

  /**
   * Defines logic for handling [ProfileEvent] [ProfileSelection.ProfileSelectionCompleted].
   * Caches the bookRegistry and subscribes to [BookEvent]s.
   * When the [ProfileEvent] [ProfileSelection.ProfileSelectionCompleted] fires,
   * the registry will represent what is currently on disc.
   */
  private fun onProfileSelectionCompleted() {
    this.logger.debug("NotificationsService::onProfileSelectionCompleted")
    this.subscribeToBookEvents()
  }

  /**
   * Subscribes [NotificationsService] to receive [BookStatusEvent]s
   */
  private fun subscribeToBookEvents() {
    this.logger.debug("NotificationsService::subscribeToBookEvents")
    this.bookRegistrySubscription =
      this.bookRegistry.bookEvents()
        .ofType(BookStatusEvent.BookStatusEventChanged::class.java)
        .observeOn(Schedulers.from(this.executor))
        .subscribe(this::onBookEvent)
  }

  private fun onBookEvent(event: BookStatusEvent.BookStatusEventChanged) {
    if (this.statusChangedSufficiently(event.statusPrevious, event.statusNow)) {
      this.publishNotification(
        this.notificationResourcesType.titleReadyNotificationTitle,
        this.notificationResourcesType.titleReadyNotificationContent
      )
    }
  }

  /**
   * Unsubscribes [NotificationsService] from [BookStatusEvent]s
   */
  private fun unsubscribeFromBookEvents() {
    this.logger.debug("NotificationsService::unsubscribeFromBookEvents")
    this.bookRegistrySubscription?.dispose()
  }

  /**
   * Checks that the new status satisfies the rules for prompting us to show a notification.
   */
  private fun statusChangedSufficiently(statusBefore: BookStatus?, statusNow: BookStatus): Boolean {
    // Compare statusBefore and statusNow, only return true if statusNow is actually [BookStatusHeldReady]
    this.logger.debug("NotificationsService::statusChangedSufficiently comparing $statusBefore to $statusNow")
    return statusBefore is BookStatus.Held.HeldInQueue && statusNow is BookStatus.Held.HeldReady
  }

  /**
   * Returns a map of the books in the [BookRegistry]
   */
  private fun getBookStatusesFromRegistry(): Map<BookID, BookWithStatus> {
    this.logger.debug("NotificationsService::getBookStatusesFromRegistry")
    this.logger.debug("bookRegistry has ${this.bookRegistry.books().size} books")
    return this.bookRegistry.books().toMap()
  }

  /**
   * Calls to the [NotificationsWrapper] to post a notification.
   */
  private fun publishNotification(notificationTitle: String, notificationContent: String) {
    this.logger.debug(
      "NotificationsService::publishNotification " +
        "with title $notificationTitle and content $notificationContent"
    )
    this.notificationsWrapper.postDefaultNotification(this.notificationResourcesType)
  }
}
