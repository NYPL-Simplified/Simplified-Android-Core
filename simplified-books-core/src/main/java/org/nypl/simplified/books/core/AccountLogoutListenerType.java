package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account logout operations.
 */

public interface AccountLogoutListenerType
{
  /**
   * Logging out failed.
   *
   * @param error   The exception, if any
   * @param message The error message
   */

  void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message);

  /**
   * Logging out succeeded.
   */

  void onAccountLogoutSuccess();

  /**
   * Logout failed
   * @param code error code
   */
  void onAccountLogoutFailureServerError(final int code);

}
