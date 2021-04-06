package org.nypl.simplified.profiles.api.idle_timer;

/**
 * The type of profile idle timers.
 * <p>
 * When a timer is started, it counts out exactly {@code n} seconds before publishing a
 * {@link ProfileIdleTimedOut} event. The timer can be reset each time the user performs some
 * action (such as interacting with the UI) that indicates that the profile is in fact still
 * active. The timer will publish a {@link ProfileIdleTimeOutSoon} event a configurable number of
 * seconds before any {@link ProfileIdleTimedOut} event is published in order to give any user that
 * happens to be watching a chance to reset the timer by interacting with the UI.
 */

public interface ProfileIdleTimerType {

  /**
   * Start the timer.
   */

  void start();

  /**
   * Stop the timer.
   */

  void stop();

  /**
   * Reset the elapsed time of the timer to the current maximum.
   */

  void reset();

  /**
   * Specify how many seconds prior to the actual {@link ProfileIdleTimedOut} event a
   * {@link ProfileIdleTimeOutSoon} event should be published.
   *
   * @param time The idle time
   */

  void setWarningIdleSecondsRemaining(int time);

  /**
   * Set the maximum idle time in seconds.
   *
   * @param time The idle time
   */

  void setMaximumIdleSeconds(int time);

  /**
   * Set the maximum idle time in minutes.
   *
   * @param minutes The idle time in minutes
   */

  default void setMaximumIdleMinutes(
    final int minutes) {
    this.setMaximumIdleSeconds(minutes * 60);
  }

  /**
   * @return The current maximum idle time
   */

  int maximumIdleSeconds();

  /**
   * @return The number of seconds that the profile has been idle
   */

  int currentIdleSeconds();
}
