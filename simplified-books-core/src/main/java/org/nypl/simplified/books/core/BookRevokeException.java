package org.nypl.simplified.books.core;

/**
 * An exception indicating that book revoking failed.
 */

public abstract class BookRevokeException extends BookException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BookRevokeException(final Throwable cause)
  {
    super(cause);
  }

  /**
   * Construct a message.
   *
   * @param message The message
   */

  public BookRevokeException(final String message)
  {
    super(message);
  }
}
