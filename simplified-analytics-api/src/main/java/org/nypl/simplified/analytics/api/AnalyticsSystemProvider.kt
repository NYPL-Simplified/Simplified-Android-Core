package org.nypl.simplified.analytics.api

/**
 * The interface exposed by analytics system providers. Implementations are expected to make
 * themselves discoverable via [java.util.ServiceLoader]. This is effectively a _service provider
 * interface_ and therefore is not expected to be called by application code directly.
 */

interface AnalyticsSystemProvider {

  /**
   * Create a new analytics system.
   */

  fun create(configuration: AnalyticsConfiguration): AnalyticsSystem
}
