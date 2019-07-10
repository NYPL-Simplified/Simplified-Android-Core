package org.nypl.simplified.books.controller.api;

/**
 * An exception indicating that book borrowing failed because the book requires credentials to
 * borrow and the user doesn't have any.
 */

public final class BookBorrowExceptionNoCredentials
  extends BookBorrowException
{

  /**
   * Construct an exception.
   */

  public BookBorrowExceptionNoCredentials()
  {
    super("No exception!");
  }

}
