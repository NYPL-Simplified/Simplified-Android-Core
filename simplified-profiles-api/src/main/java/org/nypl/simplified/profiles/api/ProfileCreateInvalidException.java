package org.nypl.simplified.profiles.api;

import java.util.Collections;

/**
 * An exception raised when the user tries to create an invalid profile.
 */

public final class ProfileCreateInvalidException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileCreateInvalidException(final String message) {
    super(message, Collections.emptyList());
  }
}
