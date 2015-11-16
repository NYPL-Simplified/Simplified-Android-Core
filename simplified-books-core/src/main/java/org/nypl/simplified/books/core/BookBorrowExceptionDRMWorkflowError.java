package org.nypl.simplified.books.core;

/**
 * An exception indicating that book borrowing failed because the DRM client failed
 * when executing its workflow.
 */

public final class BookBorrowExceptionDRMWorkflowError extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param error_code The error code
   */

  public BookBorrowExceptionDRMWorkflowError(final String error_code)
  {
    super(error_code);
  }
}
