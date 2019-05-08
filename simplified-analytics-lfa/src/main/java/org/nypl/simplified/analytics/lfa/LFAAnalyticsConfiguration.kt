package org.nypl.simplified.analytics.lfa

import java.io.InputStream
import java.net.URI
import java.util.Properties

/**
 * Configuration information for LFA analytics.
 */

data class LFAAnalyticsConfiguration(

  /**
   * The target URI of the analytics server.
   */

  val targetURI: URI,

  /**
   * The token needed to speak with the analytics server.
   */

  val token: String,

  /**
   * A unique ID for this device.
   */

  val deviceID: String,

  /**
   * The maximum size of a log file in bytes.
   */

  val logFileSizeLimit: Int = 1024 * 1024 * 10) {

  companion object {

    fun loadFrom(stream: InputStream, deviceID: String): LFAAnalyticsConfiguration {
      val properties = Properties()
      properties.load(stream)
      return LFAAnalyticsConfiguration(
        targetURI = URI.create(properties.getProperty("one.lfa.analytics.targetURI")),
        deviceID = deviceID,
        token = properties.getProperty("one.lfa.analytics.token"))
    }

  }

}
