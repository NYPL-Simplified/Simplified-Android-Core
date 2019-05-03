package org.nypl.simplified.books.controller.api;

import com.io7m.jnull.NullCheck;

/**
 * An exception indicating that a book cannot be fulfilled because of an
 * unsupported type.
 */

public final class BookUnsupportedTypeException extends BookException
{
  private final String type;

  /**
   * Construct an exception.
   *
   * @param in_type The unsupported file type
   */

  public BookUnsupportedTypeException(final String in_type)
  {
    super(in_type);
    this.type = NullCheck.notNull(in_type);
  }

  /**
   * @return The type of unsupported book
   */

  public String getType()
  {
    return this.type;
  }
}
