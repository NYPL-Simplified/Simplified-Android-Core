package org.nypl.simplified.profiles.api;

import java.util.Collections;

/**
 * An exception caused by an attempt to delete the anonymous profile.
 */

public final class ProfileDatabaseDeleteAnonymousException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileDatabaseDeleteAnonymousException(final String message) {
    super(message, Collections.emptyList());
  }
}
