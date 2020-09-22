package org.nypl.simplified.analytics.circulation

import com.io7m.jfunctional.Option
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
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

  private fun consumeEvent(event: AnalyticsEvent) {
    this.logger.debug("received event {}", event::class.simpleName)
    when (event) {
      is AnalyticsEvent.BookOpened -> {
        event.targetURI?.let { target ->
          postURI(target, event.credentials)
        }
        this.logger.debug("consuming 'BookOpened' event for {}", event.targetURI)
      }
      else -> {
        // All other events are silently dropped
      }
    }
  }

  private fun postURI(
    target: URI,
    credentials: AccountAuthenticationCredentials?
  ) {
    val httpAuth =
      if (credentials != null) {
        Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials))
      } else {
        Option.none()
      }

    val result =
      this.configuration.http.get(httpAuth, target, 0L, true)

    return result.match<Unit, Exception>(
      { error ->
        HTTPProblemReportLogging.logError(
          this.logger,
          target,
          error.message,
          error.status,
          error.problemReport
        )
      },
      { exception ->
        this.logger.error(
          "error sending event to {}: ",
          exception.uri,
          exception.error
        )
      },
      {
      }
    )
  }
}
