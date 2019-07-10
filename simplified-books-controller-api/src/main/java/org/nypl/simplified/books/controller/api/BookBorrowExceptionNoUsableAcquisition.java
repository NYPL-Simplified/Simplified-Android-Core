package org.nypl.simplified.books.controller.api;

/**
 * An exception indicating that book borrowing failed there were no acquisition
 * links that the application knows how to handle.
 */

public final class BookBorrowExceptionNoUsableAcquisition extends BookBorrowException
{
  /**
   * Construct an exception.
   */

  public BookBorrowExceptionNoUsableAcquisition()
  {
    super("No links!");
  }
}
