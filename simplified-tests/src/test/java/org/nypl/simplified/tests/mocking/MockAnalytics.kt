package org.nypl.simplified.tests.mocking

import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType

class MockAnalytics : AnalyticsType {
  override fun publishEvent(event: AnalyticsEvent) {
  }
}
