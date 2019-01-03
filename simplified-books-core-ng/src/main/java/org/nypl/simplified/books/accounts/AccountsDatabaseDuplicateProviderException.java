package org.nypl.simplified.books.accounts;

import java.util.Collections;

/**
 * An exception indicating that a user attempted to create an account using a provider that is
 * already in use.
 */

public final class AccountsDatabaseDuplicateProviderException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public AccountsDatabaseDuplicateProviderException(final String message) {
    super(message, Collections.emptyList());
  }

}
