package org.nypl.simplified.tests.books

import org.nypl.simplified.books.core.DeviceActivationListenerType
import org.slf4j.Logger

/**
 * A device activation listener that simply logs everything.
 */

open class LoggingDeviceListener(private val logger: Logger) : DeviceActivationListenerType {

  override fun onDeviceActivationFailure(message: String) {
    this.logger.error("onDeviceActivationFailure: {}", message)
  }

  override fun onDeviceActivationSuccess() {
    this.logger.debug("onDeviceActivationSuccess")
  }
}
