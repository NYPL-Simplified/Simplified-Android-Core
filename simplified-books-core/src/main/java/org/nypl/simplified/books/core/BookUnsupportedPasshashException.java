package org.nypl.simplified.books.core;

/**
 * An exception indicating that a book cannot be fulfilled because it is
 * protected by the unsupported "passhash" protection.
 */

public final class BookUnsupportedPasshashException extends Exception
{
  /**
   * Construct an exception.
   */

  public BookUnsupportedPasshashException()
  {
    super();
  }
}
