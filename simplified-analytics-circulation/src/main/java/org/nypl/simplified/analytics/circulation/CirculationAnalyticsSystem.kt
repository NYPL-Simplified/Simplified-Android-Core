package org.nypl.simplified.analytics.circulation

import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.accounts.api.setAuthentication
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsSystem
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
          postURI(target, event.account)
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
    account: AccountReadableType?
  ) {
    val request =
      this.configuration.http.newRequest(target)
        .apply { account?.let { setAuthentication(account) } }
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        Unit
      is LSHTTPResponseStatus.Responded.Error -> {
        val problemReport = status.properties.problemReport
        if (problemReport != null) {
          this.logger.debug("status: {}", problemReport.status)
          this.logger.debug("title:  {}", problemReport.title)
          this.logger.debug("type:   {}", problemReport.type)
          this.logger.debug("detail: {}", problemReport.detail)
        } else {
          Unit
        }
      }
      is LSHTTPResponseStatus.Failed ->
        this.logger.error("error sending event to {}: ", target, status.exception)
    }
  }
}
