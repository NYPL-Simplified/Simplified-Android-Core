package org.nypl.simplified.books.audio

/**
 * A provider of strategies to fulfill/parse/license check manifests.
 */

interface AudioBookManifestStrategiesType {

  /**
   * Create a new strategy based on the given request. Once a strategy is created, it may be
   * executed any number of times and should (in the absence of errors) produce the same result
   * each time it is executed.
   */

  fun createStrategy(
    request: AudioBookManifestRequest
  ): AudioBookManifestStrategyType
}
