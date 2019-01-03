package org.nypl.simplified.books.accounts;

import java.util.Collections;

/**
 * An exception indicating that a user attempted to find a provider that does not exist.
 */

public final class AccountsDatabaseNonexistentProviderException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public AccountsDatabaseNonexistentProviderException(final String message) {
    super(message, Collections.emptyList());
  }

}
