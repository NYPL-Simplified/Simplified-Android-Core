package org.nypl.simplified.books.core;

/**
 * The given PIN was rejected.
 */

public final class AccountAuthenticationPINRejectedError extends
  AccountAuthenticationError
{
  private static final long serialVersionUID = 1L;

  public AccountAuthenticationPINRejectedError(
    final String message)
  {
    super(message);
  }
}
