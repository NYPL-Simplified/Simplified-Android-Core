package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for account sync operations.
 *
 * If authentication fails, {@link #onAccountSyncAuthenticationFailure(String)}
 * is called. Otherwise, for each book in the account {@link
 * #onAccountSyncBook(BookID)} or is called, followed by {@link
 * #onAccountSyncSuccess()}. Otherwise, {@link #onAccountSyncFailure(OptionType,
 * String)} is called.
 */

public interface AccountSyncListenerType
{
  /**
   * Authentication failed.
   *
   * @param message The error message
   */

  void onAccountSyncAuthenticationFailure(
    String message);

  /**
   * Synchronizing the given book was successful.
   *
   * @param book The book ID
   */

  void onAccountSyncBook(
    BookID book);

  /**
   * Synchronizing failed.
   *
   * @param error   The exception, if any
   * @param message The error message
   */

  void onAccountSyncFailure(
    OptionType<Throwable> error,
    String message);

  /**
   * Synchronizing finished successfully.
   */

  void onAccountSyncSuccess();
}
