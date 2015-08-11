package org.nypl.simplified.books.core;

/**
 * The type of exceptions indicating authentication errors when attempting to
 * access an account.
 */

public abstract class AccountAuthenticationError extends Exception
{
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public AccountAuthenticationError(
    final String message)
  {
    super(message);
  }
}
