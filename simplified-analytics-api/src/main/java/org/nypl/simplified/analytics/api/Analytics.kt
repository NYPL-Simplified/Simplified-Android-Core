package org.nypl.simplified.analytics.api

import android.content.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * The default analytics API.
 */

class Analytics private constructor(
  private val logger: Logger,
  private val consumers: List<AnalyticsSystem>) : AnalyticsType {

  companion object {

    private val LOG = LoggerFactory.getLogger(Analytics::class.java)

    /**
     * Create a new analytics API, loading all available systems from [ServiceLoader].
     */

    fun create(context: Context): AnalyticsType {
      return Analytics(LOG,
        ServiceLoader.load(AnalyticsSystemProvider::class.java)
          .toList()
          .map { provider -> provider.create(context) }
          .map { system ->
            LOG.debug("created analytics system: ${system::class.java.canonicalName}")
            system
          })
    }
  }

  override fun publishEvent(event: AnalyticsEvent) =
    this.consumers.forEach { system ->
      try {
        system.onAnalyticsEvent(event)
      } catch (e: Exception) {
        this.logger.error("failed to publish analytics event: ", e)
      }
    }

}