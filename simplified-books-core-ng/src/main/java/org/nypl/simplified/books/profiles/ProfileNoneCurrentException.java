package org.nypl.simplified.books.profiles;

import java.util.Collections;

/**
 * An exception raised when the user tries to operate on the current profile despite there not
 * being a current profile selected.
 */

public final class ProfileNoneCurrentException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileNoneCurrentException(final String message) {
    super(message, Collections.emptyList());
  }
}
