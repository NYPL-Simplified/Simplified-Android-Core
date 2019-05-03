package org.nypl.simplified.books.controller.api;

/**
 * An exception indicating that book revoking failed.
 */

public abstract class BookRevokeException extends BookException
{
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
