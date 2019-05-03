package org.nypl.simplified.profiles.api.idle_timer;

import com.google.auto.value.AutoValue;

import org.nypl.simplified.profiles.api.ProfileEvent;

/**
 * An event indicating that a profile has been idle for a time and will soon be logged out.
 */

@AutoValue
public abstract class ProfileIdleTimeOutSoon extends ProfileEvent {

  /**
   * @return An event
   */

  public static ProfileIdleTimeOutSoon get() {
    return new AutoValue_ProfileIdleTimeOutSoon();
  }
}
