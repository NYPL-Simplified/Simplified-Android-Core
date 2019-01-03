package org.nypl.simplified.books.accounts;

import org.nypl.simplified.books.book_database.BookDatabaseException;

import java.util.Collections;

/**
 * An exception indicating an underlying books database exception.
 */

public final class AccountsDatabaseBooksException extends AccountsDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   * @param cause   The cause
   */

  public AccountsDatabaseBooksException(
      final String message,
      final BookDatabaseException cause) {
    super(message, cause, Collections.singletonList(cause));
  }

}
