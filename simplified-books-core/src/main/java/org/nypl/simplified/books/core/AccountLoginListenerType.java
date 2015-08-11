package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account login operations.
 */

public interface AccountLoginListenerType
{
  /**
   * Logging in failed.
   *
   * @param error   The exception, if any
   * @param message The error message
   */

  void onAccountLoginFailure(
    OptionType<Throwable> error,
    String message);

  /**
   * Logging in succeeded.
   *
   * @param barcode The current barcode
   * @param pin     The current PIN
   */

  void onAccountLoginSuccess(
    AccountBarcode barcode,
    AccountPIN pin);
}
