package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners for book borrowing operations.
 */

public interface BookBorrowListenerType
{
  /**
   * Book borrowing failed.
   *
   * @param id The book ID
   * @param e  The exception, if any
   */

  void onBookBorrowFailure(
    BookID id,
    OptionType<Throwable> e);

  /**
   * Book borrowing succeeded.
   *
   * @param id The book ID
   */

  void onBookBorrowSuccess(
    BookID id);
}
