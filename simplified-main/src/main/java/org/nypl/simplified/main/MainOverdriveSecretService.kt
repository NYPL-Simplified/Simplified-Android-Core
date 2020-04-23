package org.nypl.simplified.main

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import org.nypl.simplified.books.audio.AudioBookOverdriveSecretServiceType
import org.nypl.simplified.json.core.JSONParserUtilities
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

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
        context.assets.open("overdrive.json").use(::create)
      } catch (e: FileNotFoundException) {
        this.logger.debug("Overdrive configuration not present: ", e)
        null
      } catch (e: IOException) {
        this.logger.debug("could not initialize Overdrive secret service: ", e)
        throw IllegalStateException("could not initialize Overdrive secret service", e)
      }
    }

    fun create(
      stream: InputStream
    ): AudioBookOverdriveSecretServiceType {
      val objectMapper = ObjectMapper()
      val root = objectMapper.readTree(stream)
      val rootObject =
        JSONParserUtilities.checkObject(null, root)
      val clientKey =
        JSONParserUtilities.getString(rootObject, "clientKey")
      val clientPass =
        JSONParserUtilities.getString(rootObject, "clientSecret")
      return MainOverdriveSecretService(
        clientKey = clientKey,
        clientPass = clientPass
      )
    }
  }
}
