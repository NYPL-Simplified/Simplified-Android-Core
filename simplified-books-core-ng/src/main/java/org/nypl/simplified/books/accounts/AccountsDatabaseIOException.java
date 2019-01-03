package org.nypl.simplified.books.accounts;

import java.io.IOException;
import java.util.Collections;

/**
 * An exception indicating an underlying I/O exception in a database.
 */

public final class AccountsDatabaseIOException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   * @param cause   The cause
   */

  public AccountsDatabaseIOException(
      final String message,
      final IOException cause) {
    super(message, cause, Collections.singletonList(cause));
  }

}
