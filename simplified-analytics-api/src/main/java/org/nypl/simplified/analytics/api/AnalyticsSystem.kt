package org.nypl.simplified.analytics.api

/**
 * The interface exposed by analytics systems. This is effectively a _service provider interface_
 * and therefore is not expected to be called by application code directly.
 */

interface AnalyticsSystem {

  /**
   * An event occurred. Consume the event if possible, or ignore it otherwise. Event consumption
   * _must_ be asynchronous; calling this method must not block the caller.
   */

  fun onAnalyticsEvent(
    event: AnalyticsEvent)

}