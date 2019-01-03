package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book borrowing failed because the DRM client failed
 * when executing its workflow.
 */

public final class AccountTooManyActivationsException extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param error_code The error code
   */

  public AccountTooManyActivationsException(final String error_code)
  {
    super(error_code);
  }
}
