package org.nypl.simplified.tests

import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType

class MockAnalytics : AnalyticsType {
  override fun publishEvent(event: AnalyticsEvent) {
  }
}
