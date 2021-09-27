package org.nypl.simplified.metrics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.nypl.simplified.metrics.api.MetricEvent
import org.nypl.simplified.metrics.api.MetricServiceType

class MetricService internal constructor(context: Context) : MetricServiceType {
  private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }
  override fun logMetric(event: MetricEvent) {
    val args = when (event) {
      is MetricEvent.LibraryAdded -> Bundle().apply { putString("library_id", event.id) }
      is MetricEvent.LibraryRemoved -> Bundle().apply { putString("library_id", event.id) }
      else -> null
    }
    analytics.logEvent(event.key, args)
  }
}
