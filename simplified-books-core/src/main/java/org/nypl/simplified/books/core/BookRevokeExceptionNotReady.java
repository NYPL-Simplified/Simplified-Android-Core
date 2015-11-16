package org.nypl.simplified.books.core;

/**
 * An exception indicating that book revoking failed because revoking is still in progress.
 */

public final class BookRevokeExceptionNotReady extends BookBorrowException
{
  /**
   * Construct an exception.
   */
  public BookRevokeExceptionNotReady()
  {
    super("Revoke still in progress!");
  }
}
