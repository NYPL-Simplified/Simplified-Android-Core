package org.nypl.simplified.analytics.circulation

import com.io7m.jfunctional.Option
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsSystem
import org.nypl.simplified.http.core.HTTPProblemReportLogging
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ExecutorService

/**
 * An analytics system based on Circulation Analytics.
 */

class CirculationAnalyticsSystem(
  private val configuration: AnalyticsConfiguration,
  private val executor: ExecutorService
) : AnalyticsSystem {

  private val logger =
    LoggerFactory.getLogger(CirculationAnalyticsSystem::class.java)

  override fun onAnalyticsEvent(event: AnalyticsEvent): Unit =
    this.executor.execute { this.consumeEvent(event) }

  private fun consumeEvent(event: AnalyticsEvent): Unit =
    when (event) {
      is AnalyticsEvent.SyncRequested,
      is AnalyticsEvent.BookPageTurned,
      is AnalyticsEvent.BookClosed,
      is AnalyticsEvent.ApplicationOpened,
      is AnalyticsEvent.ProfileLoggedIn,
      is AnalyticsEvent.ProfileLoggedOut,
      is AnalyticsEvent.ProfileCreated,
      is AnalyticsEvent.ProfileDeleted,
      is AnalyticsEvent.ProfileUpdated,
      is AnalyticsEvent.CatalogSearched -> {

        /*
         * All of these event types are ignored.
         */
      }

      is AnalyticsEvent.BookOpened -> {

        /*
         * The user opened a book. Touch the URI that the book (hopefully) included.
         */

        val targetURI = event.targetURI
        if (targetURI != null) {
          postURI(event, targetURI)
        } else {
          this.logger.debug(
            "no analytics URI available for book {} ({})",
            event.opdsEntry.id,
            event.opdsEntry.title)
        }
      }
    }

  private fun postURI(
    event: AnalyticsEvent,
    targetURI: URI
  ) {
    val credentials = event.credentials
    val httpAuth =
      if (credentials != null) {
        Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials))
      } else {
        Option.none()
      }

    val result =
      this.configuration.http.get(httpAuth, targetURI, 0L)

    return result.match<Unit, Exception>({ error ->
      HTTPProblemReportLogging.logError(
        this.logger,
        targetURI,
        error.message,
        error.status,
        error.problemReport)
    },
      { exception ->
        this.logger.error(
          "error sending event to {}: ",
          exception.uri,
          exception.error)
      },
      {
      })
  }
}
