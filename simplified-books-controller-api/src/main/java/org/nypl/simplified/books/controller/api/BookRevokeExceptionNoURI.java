package org.nypl.simplified.books.controller.api;

/**
 * An exception indicating that book revoking failed because the book doesn't have a URI that can
 * be used to revoke it.
 */

public final class BookRevokeExceptionNoURI extends BookRevokeException
{
  /**
   * Construct an exception.
   */

  public BookRevokeExceptionNoURI()
  {
    super("No available revocation URI");
  }
}
