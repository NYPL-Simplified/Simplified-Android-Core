package org.nypl.simplified.books.profiles;

import com.google.auto.value.AutoValue;

/**
 * A unique profile identifier.
 */

@AutoValue
public abstract class ProfileID implements Comparable<ProfileID> {

  ProfileID() {

  }

  /**
   * Construct a profile identifier.
   *
   * @param id A non-negative integer
   */

  public static ProfileID create(int id) {
    if (id < 0) {
      throw new IllegalArgumentException("Profile identifiers must be non-negative");
    }
    return new AutoValue_ProfileID(id);
  }

  /**
   * @return The raw identifier value
   */

  public abstract int id();

  @Override
  public int compareTo(ProfileID other) {
    return Integer.compare(this.id(), other.id());
  }
}
