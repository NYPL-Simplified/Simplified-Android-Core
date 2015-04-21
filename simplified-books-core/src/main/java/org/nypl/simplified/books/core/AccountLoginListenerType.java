package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account login operations.
 */

public interface AccountLoginListenerType
{
  /**
   * Logging in failed.
   */

  void onAccountLoginFailure(
    OptionType<Throwable> error,
    String message);

  /**
   * Logging in succeeded.
   */

  void onAccountLoginSuccess(
    AccountBarcode barcode,
    AccountPIN pin);
}
