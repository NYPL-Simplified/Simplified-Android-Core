package org.nypl.simplified.books.core;

public final class AccountAuthenticationPINNotGivenError extends
  AccountAuthenticationError
{
  private static final long serialVersionUID = 1L;

  public AccountAuthenticationPINNotGivenError(
    final String message)
  {
    super(message);
  }
}
