package org.nypl.simplified.analytics.api

/**
 * The API for publishing analytics events.
 */

interface AnalyticsType {

  /**
   * Publish an event. All available analytics systems will be presented with the event and
   * will process them however they desire.
   */

  fun publishEvent(event: AnalyticsEvent)

}