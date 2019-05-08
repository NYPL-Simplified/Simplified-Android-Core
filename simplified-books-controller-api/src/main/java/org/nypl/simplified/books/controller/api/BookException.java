package org.nypl.simplified.books.controller.api;

/**
 * An exception indicating that something to do with a book failed.
 */

public abstract class BookException extends Exception
{
  /**
   * Construct an exception.
   *
   * @param message The message
   */

  public BookException(final String message)
  {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param message   The message
   * @param throwable The cause
   */

  public BookException(
    final String message,
    final Throwable throwable)
  {
    super(message, throwable);
  }

  /**
   * Construct an exception.
   *
   * @param throwable The cause
   */

  public BookException(final Throwable throwable)
  {
    super(throwable);
  }
}
