package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account sync operations.
 *
 * If authentication fails,
 * {@link #onAccountSyncAuthenticationFailure(String)} is called. Otherwise,
 * for each book in the account {@link #onAccountSyncBook(BookID)} or is
 * called, followed by {@link #onAccountSyncSuccess()}. Otherwise,
 * {@link #onAccountSyncFailure(OptionType, String)} is called.
 */

public interface AccountSyncListenerType
{
  /**
   * Authentication failed.
   */

  void onAccountSyncAuthenticationFailure(
    String message);

  /**
   * Synchronizing the given book was successful.
   */

  void onAccountSyncBook(
    BookID book);

  /**
   * Synchronizing failed.
   */

  void onAccountSyncFailure(
    OptionType<Throwable> error,
    String message);

  /**
   * Synchronizing finished successfully.
   */

  void onAccountSyncSuccess();
}
