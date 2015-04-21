package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for book borrowing operations.
 */

public interface BookBorrowListenerType
{
  /**
   * Book borrowing failed.
   */

  void onBookBorrowFailure(
    BookID id,
    OptionType<Throwable> e);

  /**
   * Book borrowing succeeded.
   */

  void onBookBorrowSuccess(
    BookID id);
}
