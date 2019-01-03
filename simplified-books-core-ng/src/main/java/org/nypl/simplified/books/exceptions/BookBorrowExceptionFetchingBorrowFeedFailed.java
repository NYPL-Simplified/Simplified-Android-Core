package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book borrowing failed because the server
 * responded with an error when the <tt>borrow</tt> link was requested.
 */

public final class BookBorrowExceptionFetchingBorrowFeedFailed extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BookBorrowExceptionFetchingBorrowFeedFailed(final Throwable cause)
  {
    super(cause);
  }
}
