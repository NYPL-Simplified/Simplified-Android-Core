package org.nypl.simplified.profiles.api;

import java.util.Collections;

/**
 * An exception raised when the user tries to operate on a nonexistent profile.
 */

public final class ProfileNonexistentException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileNonexistentException(final String message) {
    super(message, Collections.emptyList());
  }
}
