package org.nypl.simplified.analytics.lfa

import android.content.Context
import org.nypl.simplified.analytics.api.AnalyticsSystem
import org.nypl.simplified.analytics.api.AnalyticsSystemProvider
import org.nypl.simplified.threads.NamedThreadPools

/**
 * An analytics system that uses LFA's "mostly-offline" analytics.
 */

class LFAAnalyticsSystems : AnalyticsSystemProvider {

  private val executor =
    NamedThreadPools.namedThreadPool(1, "circulation-analytics", 19)

  override fun create(context: Context): AnalyticsSystem =
    LFAAnalyticsSystem(context, executor)

}
