package org.nypl.simplified.books.accounts;

import com.io7m.jnull.NullCheck;

import java.util.List;

/**
 * An exception that indicates that an operation on the accounts database failed.
 */

public abstract class AccountsDatabaseException extends Exception {

  private final List<Exception> causes;

  /**
   * Construct an exception.
   *
   * @param message The exception message
   * @param causes  The list of causes
   */

  public AccountsDatabaseException(
      final String message,
      final List<Exception> causes) {
    super(NullCheck.notNull(message, "Message"));
    this.causes = NullCheck.notNull(causes, "Causes");
  }

  /**
   * Construct an exception.
   *
   * @param message The exception message
   *                @param cause The primary cause
   * @param causes  The list of causes
   */

  public AccountsDatabaseException(
      final String message,
      final Exception cause,
      final List<Exception> causes) {
    super(NullCheck.notNull(message, "Message"),
        NullCheck.notNull(cause, "Cause"));
    this.causes = NullCheck.notNull(causes, "Causes");
  }

  /**
   * @return The list of exceptions raised that caused this exception
   */

  public final List<Exception> causes() {
    return this.causes;
  }
}
