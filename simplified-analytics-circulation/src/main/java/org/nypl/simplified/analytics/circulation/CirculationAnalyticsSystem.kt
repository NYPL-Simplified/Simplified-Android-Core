package org.nypl.simplified.analytics.circulation

import android.content.Context
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsSystem
import java.util.concurrent.ExecutorService

class CirculationAnalyticsSystem(
  private val context: Context,
  private val executor: ExecutorService) : AnalyticsSystem {

  override fun onAnalyticsEvent(event: AnalyticsEvent): Unit =
    this.executor.execute { this.consumeEvent(event) }

  private fun consumeEvent(event: AnalyticsEvent) =
    when (event) {
      is AnalyticsEvent.ProfileLoggedIn -> {

      }
      is AnalyticsEvent.ProfileLoggedOut -> {

      }
      is AnalyticsEvent.CatalogSearched -> {

      }
      is AnalyticsEvent.BookOpened -> {

      }
      is AnalyticsEvent.BookPageTurned -> {

      }
      is AnalyticsEvent.BookClosed -> {

      }
    }

}