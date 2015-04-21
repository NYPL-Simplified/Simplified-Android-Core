package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for data loading operations.
 *
 * If the user has not logged into an account, {@link #onAccountUnavailable()}
 * will be called. Otherwise, for each book, either
 * {@link #onAccountDataBookLoadFailed(BookID, OptionType, String)} or
 * {@link #onAccountDataBookLoadSucceeded(BookID, BookSnapshot)} will be
 * called. Finally, {@link #onAccountDataBookLoadFinished()} will be called.
 */

public interface AccountDataLoadListenerType
{
  /**
   * Loading a particular book failed.
   */

  void onAccountDataBookLoadFailed(
    final BookID id,
    final OptionType<Throwable> error,
    final String message);

  /**
   * The loading operation has completed. Will be called regardless of whether
   * {@link #onAccountDataBookLoadFailed(BookID, OptionType, String)} or
   * {@link #onAccountDataBookLoadSucceeded(BookID, BookSnapshot)} was called.
   */

  void onAccountDataBookLoadFinished();

  /**
   * Loading a particular book succeeded.
   */

  void onAccountDataBookLoadSucceeded(
    final BookID book,
    final BookSnapshot snap);

  /**
   * The user has not logged into an account, so no data can be loaded.
   */

  void onAccountUnavailable();
}
