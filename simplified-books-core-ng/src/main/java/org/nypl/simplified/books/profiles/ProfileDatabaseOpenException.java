package org.nypl.simplified.books.profiles;

import java.util.List;

/**
 * An exception raised when opening a database.
 */

public final class ProfileDatabaseOpenException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   * @param causes  The list of causes
   */

  public ProfileDatabaseOpenException(
      final String message,
      final List<Exception> causes) {
    super(message, causes);
  }
}
