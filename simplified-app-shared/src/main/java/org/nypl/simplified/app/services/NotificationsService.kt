package org.nypl.simplified.app.services

import android.content.Context
import com.io7m.jfunctional.Some
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

        for(book in books){
            val bookId = book.key
            statuses[bookId] = (bookRegistry.book(bookId) as Some<BookWithStatus>).get()
        }

        return statuses.toMap()
    }
}
