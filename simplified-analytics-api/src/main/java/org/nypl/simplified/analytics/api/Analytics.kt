package org.nypl.simplified.analytics.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * The default analytics API.
 */

class Analytics private constructor(
  private val logger: Logger,
  private val consumers: List<AnalyticsSystem>
) : AnalyticsType {

  init {
    this.logger.debug("initialized {} analytics systems", this.consumers.size)
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(Analytics::class.java)

    /**
     * Create a new analytics API, loading all available systems from [ServiceLoader].
     */

    fun create(configuration: AnalyticsConfiguration): AnalyticsType {
      return Analytics(
        LOG,
        ServiceLoader.load(AnalyticsSystemProvider::class.java)
          .toList()
          .map { provider -> startProvider(provider, configuration) }
          .filterNotNull()
          .map { system ->
            LOG.debug("created analytics system: ${system::class.java.canonicalName}")
            system
          }
      )
    }

    private fun startProvider(
      provider: AnalyticsSystemProvider,
      configuration: AnalyticsConfiguration
    ): AnalyticsSystem? {
      return try {
        provider.create(configuration)
      } catch (e: Exception) {
        LOG.error(
          "{}: failed to start analytics system: {}",
          provider::class.java.canonicalName, e
        )
        null
      }
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
