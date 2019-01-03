package org.nypl.simplified.books.profiles;

import java.util.Collections;

/**
 * An exception raised by the user attempting to perform an operation that is disallowed when
 * the anonymous profile is enabled.
 */

public final class ProfileAnonymousEnabledException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileAnonymousEnabledException(final String message) {
    super(message, Collections.emptyList());
  }
}
