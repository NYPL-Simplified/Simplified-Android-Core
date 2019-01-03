package org.nypl.simplified.books.profiles;

import com.google.auto.value.AutoValue;

/**
 * A profile was selected.
 */

@AutoValue
public abstract class ProfileSelected extends ProfileEvent {

  /**
   * @return A new event
   */

  public static ProfileSelected of()
  {
    return new AutoValue_ProfileSelected();
  }
}
