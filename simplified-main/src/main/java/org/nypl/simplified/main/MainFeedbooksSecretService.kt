package org.nypl.simplified.main

import android.content.Context
import android.util.Base64
import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.NullPointerException
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
        context.assets.open("secrets.conf").use(::create)
      } catch (e: FileNotFoundException) {
        this.logger.warn("failed to initialize Feedbooks; secrets.conf not found")
        null
      } catch (e: Exception) {
        this.logger.warn("failed to initialize Feedbooks", e)
        null
      }
    }

    fun create(
      stream: InputStream
    ): AudioBookFeedbooksSecretServiceType {
      val properties =
        Properties().apply { load(stream) }

      val issuerURI =
        properties.getProperty("feedbooks.prod.issuer")
          ?: throw NullPointerException("feedbooks.prod.issuer is missing")
      val sharedSecret =
        properties.getProperty("feedbooks.prod.secret")
          ?: throw NullPointerException("feedbooks.prod.secret is missing")
      val decodedSecret =
        Base64.decode(sharedSecret, Base64.DEFAULT)

      return MainFeedbooksSecretService(
        FeedbooksPlayerExtensionConfiguration(
          bearerTokenSecret = decodedSecret,
          issuerURL = issuerURI
        )
      )
    }
  }
}
