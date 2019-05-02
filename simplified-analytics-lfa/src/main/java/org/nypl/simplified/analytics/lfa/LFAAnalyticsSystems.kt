package org.nypl.simplified.analytics.lfa

import android.content.Context
import android.provider.Settings
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsSystem
import org.nypl.simplified.analytics.api.AnalyticsSystemProvider
import org.nypl.simplified.threads.NamedThreadPools
import java.io.File

/**
 * An analytics system that uses LFA's "mostly-offline" analytics.
 */

class LFAAnalyticsSystems : AnalyticsSystemProvider {

  private val executor =
    NamedThreadPools.namedThreadPool(1, "circulation-analytics", 19)

  private fun loadConfiguration(
    context: Context,
    deviceID: String): LFAAnalyticsConfiguration =
    context.assets.open("lfa-analytics.conf").use { stream ->
      LFAAnalyticsConfiguration.loadFrom(stream, deviceID)
    }

  override fun create(configuration: AnalyticsConfiguration): AnalyticsSystem {
    val deviceID =
      Settings.Secure.getString(
        configuration.context.getContentResolver(),
        Settings.Secure.ANDROID_ID)

    val directory =
      File(configuration.context.filesDir, "lfa-analytics")

    val lfaConfiguration =
      this.loadConfiguration(configuration.context, deviceID)

    return LFAAnalyticsSystem(
      baseConfiguration = configuration,
      lfaConfiguration = lfaConfiguration,
      baseDirectory = directory,
      executor = this.executor)
  }
}
