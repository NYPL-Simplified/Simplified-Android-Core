package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that a book cannot be fulfilled because it is
 * protected by the unsupported "passhash" protection.
 */

public final class BookUnsupportedPasshashException extends BookException
{
  /**
   * Construct an exception.
   */

  public BookUnsupportedPasshashException()
  {
    super("Not supported");
  }
}
