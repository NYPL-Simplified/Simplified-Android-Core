package org.nypl.simplified.books.profiles;

import java.util.Collections;

/**
 * An exception raised when the user tries to do something with the anonymous profile despite
 * it not being enabled.
 */

public final class ProfileAnonymousDisabledException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileAnonymousDisabledException(final String message) {
    super(message, Collections.emptyList());
  }
}
