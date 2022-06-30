package org.nypl.simplified.viewer.audiobook.ui.util

/**
 * The playback rate of the player.
 */

enum class PlayerPlaybackRate(val speed: Double) {

  /**
   * 75% speed.
   */

  THREE_QUARTERS_TIME(0.75),

  /**
   * Normal speed.
   */

  NORMAL_TIME(1.0),

  /**
   * 125% speed.
   */

  ONE_AND_A_QUARTER_TIME(1.25),

  /**
   * 150% speed.
   */

  ONE_AND_A_HALF_TIME(1.50),

  /**
   * 200% speed.
   */

  DOUBLE_TIME(2.0);

  /**
   * @return The speed below this speed (or the current speed if there is no lower speed)
   */

  fun decrease(): PlayerPlaybackRate {
    return when (this) {
      THREE_QUARTERS_TIME -> THREE_QUARTERS_TIME
      NORMAL_TIME -> THREE_QUARTERS_TIME
      ONE_AND_A_QUARTER_TIME -> NORMAL_TIME
      ONE_AND_A_HALF_TIME -> ONE_AND_A_QUARTER_TIME
      DOUBLE_TIME -> ONE_AND_A_HALF_TIME
    }
  }

  /**
   * @return The speed above this speed (or the current speed if there is no higher speed)
   */

  fun increase(): PlayerPlaybackRate {
    return when (this) {
      THREE_QUARTERS_TIME -> NORMAL_TIME
      NORMAL_TIME -> ONE_AND_A_QUARTER_TIME
      ONE_AND_A_QUARTER_TIME -> ONE_AND_A_HALF_TIME
      ONE_AND_A_HALF_TIME -> DOUBLE_TIME
      DOUBLE_TIME -> DOUBLE_TIME
    }
  }
}
