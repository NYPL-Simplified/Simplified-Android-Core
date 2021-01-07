package org.nypl.simplified.main

import android.content.Context
import org.nypl.simplified.books.audio.AudioBookOverdriveSecretServiceType
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.NullPointerException
import java.util.Objects
import java.util.Properties

/**
 * A service for providing Overdrive secrets.
 */

class MainOverdriveSecretService private constructor(
  override val clientKey: String,
  override val clientPass: String
) : AudioBookOverdriveSecretServiceType {

  companion object {

    private val logger =
      LoggerFactory.getLogger(MainOverdriveSecretService::class.java)

    fun createConditionally(
      context: Context
    ): AudioBookOverdriveSecretServiceType? {
      return try {
        context.assets.open("secrets.conf").use(::create)
      } catch (e: FileNotFoundException) {
        this.logger.warn("failed to initialize Overdrive; secrets.conf not found")
        null
      } catch (e: Exception) {
        this.logger.warn("failed to initialize Overdrive", e)
        null
      }
    }

    fun create(
      stream: InputStream
    ): AudioBookOverdriveSecretServiceType {
      val properties =
        Properties().apply { load(stream) }

      val clientKey =
        properties.getProperty("overdrive.prod.client.key")
          ?: throw NullPointerException("overdrive.prod.client.key is missing")
      val clientPass =
        properties.getProperty("overdrive.prod.client.secret")
          ?: throw NullPointerException("overdrive.prod.client.secret is missing")

      return MainOverdriveSecretService(
        clientKey = clientKey,
        clientPass = clientPass
      )
    }
  }
}
