package org.nypl.simplified.books.audio

/**
 * The default provider of manifest strategies.
 */

object AudioBookManifests : AudioBookManifestStrategiesType {
  override fun createStrategy(
    request: AudioBookManifestRequest
  ): AudioBookManifestStrategyType {
    return AudioBookManifestStrategy(request)
  }
}
