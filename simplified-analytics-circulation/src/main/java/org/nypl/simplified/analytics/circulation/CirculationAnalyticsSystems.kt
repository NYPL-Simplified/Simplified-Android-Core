package org.nypl.simplified.analytics.circulation

import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsSystem
import org.nypl.simplified.analytics.api.AnalyticsSystemProvider
import org.nypl.simplified.threads.NamedThreadPools

/**
 * An analytics system that uses the Circulation Mananger analytics.
 */

class CirculationAnalyticsSystems : AnalyticsSystemProvider {

  private val executor =
    NamedThreadPools.namedThreadPool(1, "circulation-analytics", 19)

  override fun create(configuration: AnalyticsConfiguration): AnalyticsSystem =
    CirculationAnalyticsSystem(configuration, this.executor)

}
