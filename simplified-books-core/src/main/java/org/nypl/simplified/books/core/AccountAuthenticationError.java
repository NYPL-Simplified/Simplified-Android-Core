package org.nypl.simplified.books.core;

public abstract class AccountAuthenticationError extends Exception
{
  private static final long serialVersionUID = 1L;

  public AccountAuthenticationError(
    final String message)
  {
    super(message);
  }
}
