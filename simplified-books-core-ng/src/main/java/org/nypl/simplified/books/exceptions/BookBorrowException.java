package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book borrowing failed.
 */

public abstract class BookBorrowException extends BookException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BookBorrowException(final Throwable cause)
  {
    super(cause);
  }

  /**
   * Construct a message.
   *
   * @param message The message
   */

  public BookBorrowException(final String message)
  {
    super(message);
  }
}
