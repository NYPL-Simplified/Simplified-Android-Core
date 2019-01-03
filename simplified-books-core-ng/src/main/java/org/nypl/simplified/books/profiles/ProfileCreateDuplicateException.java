package org.nypl.simplified.books.profiles;

import java.util.Collections;

/**
 * An exception raised when the user tries to create a profile that already exists.
 */

public final class ProfileCreateDuplicateException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileCreateDuplicateException(final String message) {
    super(message, Collections.emptyList());
  }
}
