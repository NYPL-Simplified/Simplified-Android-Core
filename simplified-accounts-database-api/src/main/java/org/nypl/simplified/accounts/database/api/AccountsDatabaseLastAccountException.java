package org.nypl.simplified.accounts.database.api;

import java.util.Collections;

/**
 * An exception indicating that a user attempted to delete the last account in the database (a
 * database must contain at least one account).
 */

public final class AccountsDatabaseLastAccountException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public AccountsDatabaseLastAccountException(final String message) {
    super(message, Collections.emptyList());
  }

}
