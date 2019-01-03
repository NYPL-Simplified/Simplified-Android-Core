package org.nypl.simplified.books.exceptions;

/**
 * An exception indicating that book revoking failed because the book requires credentials to revoke
 * and the user doesn't have any.
 */

public final class BookRevokeExceptionNoCredentials extends BookRevokeException
{
  /**
   * Construct an exception.
   */

  public BookRevokeExceptionNoCredentials()
  {
    super("No available credentials");
  }
}
