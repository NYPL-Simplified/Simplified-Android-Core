package org.nypl.simplified.accounts.database.api;

import java.util.List;

/**
 * An exception indicating a series of problems opening a database.
 */

public final class AccountsDatabaseOpenException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   * @param causes  The list of causes
   */
  public AccountsDatabaseOpenException(
      final String message,
      final List<Exception> causes) {
    super(message, causes);
  }

}
