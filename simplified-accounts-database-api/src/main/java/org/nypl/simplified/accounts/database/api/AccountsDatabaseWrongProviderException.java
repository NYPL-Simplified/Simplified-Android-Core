package org.nypl.simplified.accounts.database.api;

import java.util.Collections;

/**
 * An exception indicating that a user attempted to use an incorrect provider.
 */

public final class AccountsDatabaseWrongProviderException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public AccountsDatabaseWrongProviderException(final String message) {
    super(message, Collections.emptyList());
  }

}
