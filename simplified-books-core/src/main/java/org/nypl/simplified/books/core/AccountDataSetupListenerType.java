package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for the initial book database setup operation.
 */

public interface AccountDataSetupListenerType
{
  /**
   * Setting up the database failed.
   *
   * @param error   The error, if any
   * @param message The error message
   */

  void onAccountDataSetupFailure(
    final OptionType<Throwable> error,
    final String message);

  /**
   * Setting up the database succeeded.
   */

  void onAccountDataSetupSuccess();
}
