package org.nypl.simplified.app.services

import android.content.Context
import org.slf4j.LoggerFactory

/**
 * Functions used to help QA with testing the boot process.
 */

object BootTesting {

  private val logger = LoggerFactory.getLogger(BootTesting::class.java)

  /**
   * @return `true` if boot failures are enabled
   */

  fun isBootFailureEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("testing", Context.MODE_PRIVATE)
    return prefs.getBoolean("failBootForTestingPurposes", false)
  }

  /**
   * Enable or disable boot failures on the next boot.
   */

  fun enableBootFailures(
    context: Context,
    enabled: Boolean
  ) {
    this.logger.debug("enabling boot failures: {}", enabled)
    val prefs = context.getSharedPreferences("testing", Context.MODE_PRIVATE)
    try {
      prefs.edit()
        .putBoolean("failBootForTestingPurposes", enabled)
        .commit()
    } catch (e: Exception) {
      this.logger.error("could not switch off failBootForTestingPurposes!: ", e)
    }
  }

  /**
   * If QA have enabled boot failures for testing purposes... Fail the boot process.
   */

  fun failBootProcessForTestingPurposesIfRequested(context: Context) {
    if (this.isBootFailureEnabled(context)) {
      this.logger.debug("failing boot for testing purposes")
      this.enableBootFailures(context, false)
      throw java.lang.IllegalStateException("Boot process cancelled for testing purposes!")
    }
  }
}