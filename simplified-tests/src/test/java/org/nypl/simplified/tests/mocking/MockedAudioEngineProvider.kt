package org.nypl.simplified.tests.mocking

import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioEngineProviderType
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerVersion
import org.slf4j.LoggerFactory

/**
 * A mocked audio engine provider that can be used to intercept requests made by code
 * running in unit tests. Note that this class is registered via ServiceLoader and will be
 * picked up as an implementation by the player API.
 *
 * See: simplified-tests/src/main/resources/META-INF/services/org.librarysimplified.audiobook.api.PlayerAudioEngineProviderType
 */

class MockedAudioEngineProvider : PlayerAudioEngineProviderType {

  private val logger = LoggerFactory.getLogger(MockedAudioEngineProvider::class.java)

  override fun name(): String {
    return "mocked"
  }

  override fun tryRequest(request: PlayerAudioEngineRequest): PlayerAudioBookProviderType? {
    this.logger.debug("trying request: {}", request)

    val next = onNextRequest
    if (next != null) {
      return next.invoke(request)
    }
    return null
  }

  override fun version(): PlayerVersion {
    return PlayerVersion(100, 0, 0)
  }

  companion object {

    var onNextRequest: ((PlayerAudioEngineRequest) -> PlayerAudioBookProviderType?)? = null
  }
}
