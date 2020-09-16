package org.nypl.simplified.main

import android.content.Context
import android.util.Base64
import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Properties

class MainFeedbooksSecretService private constructor(
  override val configuration: FeedbooksPlayerExtensionConfiguration
) : AudioBookFeedbooksSecretServiceType {

  companion object {

    private val logger =
      LoggerFactory.getLogger(MainFeedbooksSecretService::class.java)

    fun createConditionally(
      context: Context
    ): AudioBookFeedbooksSecretServiceType? {
      return try {
        context.assets.open("feedbooks.conf").use(::create)
      } catch (e: FileNotFoundException) {
        this.logger.debug("could not initialize Feedbooks; feedbooks.conf not found")
        null
      } catch (e: IOException) {
        this.logger.debug("could not initialize Feedbooks secret service: ", e)
        throw IllegalStateException("could not initialize Feedbooks secret service", e)
      }
    }

    fun create(
      stream: InputStream
    ): AudioBookFeedbooksSecretServiceType {
      val properties = Properties()
      properties.load(stream)

      val issuerURI =
        properties.getProperty("issuerURL")
      val sharedSecret =
        Base64.decode(properties.getProperty("sharedSecret"), Base64.DEFAULT)

      return MainFeedbooksSecretService(
        FeedbooksPlayerExtensionConfiguration(
          bearerTokenSecret = sharedSecret,
          issuerURL = issuerURI
        )
      )
    }
  }
}
