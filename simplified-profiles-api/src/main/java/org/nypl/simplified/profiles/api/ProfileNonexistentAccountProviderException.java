package org.nypl.simplified.profiles.api;

import java.util.Collections;

/**
 * An exception raised when the user tries to specify an account provider that is unknown.
 */

public final class ProfileNonexistentAccountProviderException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileNonexistentAccountProviderException(final String message) {
    super(message, Collections.emptyList());
  }
}
