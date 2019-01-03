package org.nypl.simplified.books.idle_timer;

import com.google.auto.value.AutoValue;

import org.nypl.simplified.books.profiles.ProfileEvent;

/**
 * An event indicating that a profile has been idle for a given number of seconds and should
 * now be logged out.
 */

@AutoValue
public abstract class ProfileIdleTimedOut extends ProfileEvent {

  /**
   * @return An event
   */

  public static ProfileIdleTimedOut get()
  {
    return new AutoValue_ProfileIdleTimedOut();
  }
}
