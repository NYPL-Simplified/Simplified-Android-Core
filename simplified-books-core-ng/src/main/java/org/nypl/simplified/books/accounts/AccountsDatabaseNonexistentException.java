package org.nypl.simplified.books.accounts;

import java.util.Collections;

/**
 * An exception indicating that a user attempted to do something with a nonexistent account.
 */

public final class AccountsDatabaseNonexistentException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public AccountsDatabaseNonexistentException(final String message) {
    super(message, Collections.emptyList());
  }

}
