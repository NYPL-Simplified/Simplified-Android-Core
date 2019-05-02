package org.nypl.simplified.tests.sandbox

import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType

class MockAnalytics : AnalyticsType {
  override fun publishEvent(event: AnalyticsEvent) {

  }
}