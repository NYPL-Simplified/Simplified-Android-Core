package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book borrowing failed because the server
 * returned a feed for the <tt>borrow</tt> link that was either invalid or could
 * not be handled.
 */

public final class BookBorrowExceptionBadBorrowFeed extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BookBorrowExceptionBadBorrowFeed(final Throwable cause)
  {
    super(cause);
  }
}
