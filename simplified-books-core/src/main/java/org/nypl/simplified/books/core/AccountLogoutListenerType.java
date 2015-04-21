package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account logout operations.
 */

public interface AccountLogoutListenerType
{
  /**
   * Logging out failed.
   */

  void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message);

  /**
   * Logging out succeeded.
   */

  void onAccountLogoutSuccess();
}
