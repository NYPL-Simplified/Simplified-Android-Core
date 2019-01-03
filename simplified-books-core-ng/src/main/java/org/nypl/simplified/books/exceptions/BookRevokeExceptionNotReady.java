package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book revoking failed because revoking is still in progress.
 */

public final class BookRevokeExceptionNotReady extends BookRevokeException
{
  /**
   * Construct an exception.
   */
  public BookRevokeExceptionNotReady()
  {
    super("Revoke still in progress!");
  }
}
