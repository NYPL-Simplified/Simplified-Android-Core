package org.nypl.simplified.viewer.audiobook.timer

import org.joda.time.Duration

/**
 * The type of sleep timer events.
 */

sealed class PlayerSleepTimerEvent {

  /**
   * The sleep timer is stopped. This is the initial state.
   */

  object PlayerSleepTimerStopped : PlayerSleepTimerEvent()

  /**
   * The sleep timer is currently running. This state will be published frequently while the sleep
   * timer is counting down. If a duration was specified when the timer was started, the given
   * duration indicates the amount of time remaining.
   */

  data class PlayerSleepTimerRunning(
    val paused: Boolean,
    val remaining: Duration?
  ) : PlayerSleepTimerEvent()

  /**
   * The user cancelled the sleep timer countdown. If a duration was specified when the timer was
   * started, the given duration indicates the amount of time remaining.
   */

  data class PlayerSleepTimerCancelled(
    val remaining: Duration?
  ) : PlayerSleepTimerEvent()

  /**
   * The sleep timer ran to completion.
   */

  object PlayerSleepTimerFinished : PlayerSleepTimerEvent()
}
