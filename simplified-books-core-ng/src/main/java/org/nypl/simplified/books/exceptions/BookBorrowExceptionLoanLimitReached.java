package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book borrowing failed because the server
 * responded with an error when the <tt>borrow</tt> link was requested.
 */

public final class BookBorrowExceptionLoanLimitReached extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */
  public BookBorrowExceptionLoanLimitReached(final Throwable cause)
  {
    super(cause);
  }
}
