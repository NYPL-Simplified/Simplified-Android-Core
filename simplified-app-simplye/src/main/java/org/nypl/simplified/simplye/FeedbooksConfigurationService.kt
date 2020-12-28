package org.nypl.simplified.simplye

import android.util.Base64
import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties

/**
 * The Feedbooks configuration service.
 *
 * Note: This class _MUST_ have a public no-argument constructor for use in [java.util.ServiceLoader].
 */

class FeedbooksConfigurationService : AudioBookFeedbooksSecretServiceType {

  private val logger =
    LoggerFactory.getLogger(FeedbooksConfigurationService::class.java)

  companion object {
    private fun loadConfiguration(
      logger: Logger
    ): FeedbooksPlayerExtensionConfiguration {
      logger.debug("loading feedbooks configuration")

      try {
        val feedbooksPath =
          "/org/nypl/simplified/simplye/feedbooks.conf"
        val resourceURL =
          FeedbooksConfigurationService::class.java.getResource(feedbooksPath)
            ?: throw IllegalStateException("No feedbooks.conf provided at $feedbooksPath")

        resourceURL.openStream().use { stream ->
          val properties = Properties()
          properties.load(stream)

          val sharedSecret = properties.getProperty("sharedSecret")
            ?: throw IllegalStateException("No sharedSecret was provided in feedbooks.conf")
          val issuerURL = properties.getProperty("issuerURL")
            ?: throw IllegalStateException("No issuerURL was provided in feedbooks.conf")
          val sharedSecretDecoded =
            Base64.decode(sharedSecret, Base64.DEFAULT)

          return FeedbooksPlayerExtensionConfiguration(
            bearerTokenSecret = sharedSecretDecoded,
            issuerURL = issuerURL
          )
        }
      } catch (e: Exception) {
        logger.error("could not load feedbooks configuration: ", e)
        throw e
      }
    }
  }

  override val configuration: FeedbooksPlayerExtensionConfiguration
    get() = loadConfiguration(logger)
}
