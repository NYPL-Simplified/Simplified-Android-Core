package org.nypl.simplified.profiles.api.idle_timer

/**
 * A configuration service to configure the idle timer.
 */

interface ProfileIdleTimerConfigurationServiceType {

  /**
   * Specify how many seconds prior to the actual {@link ProfileIdleTimedOut} event a
   * {@link ProfileIdleTimeOutSoon} event should be published.
   */

  val warningWhenSecondsRemaining: Int

  /**
   * The maximum idle time in seconds.
   */

  val logOutAfterSeconds: Int
}
