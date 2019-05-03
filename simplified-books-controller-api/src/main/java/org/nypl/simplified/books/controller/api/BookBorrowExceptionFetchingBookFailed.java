package org.nypl.simplified.books.controller.api;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

/**
 * An exception indicating that book borrowing failed because the server failed
 * to deliver an epub file. Note that this exception only denotes a failure to
 * deliver data, it doesn't cover cases where the server did respond but
 * responded with garbage.
 */

public final class BookBorrowExceptionFetchingBookFailed
  extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BookBorrowExceptionFetchingBookFailed(final Throwable cause)
  {
    super(cause);
  }

  /**
   * Construct an exception.
   */

  public BookBorrowExceptionFetchingBookFailed()
  {
    super("No exception!");
  }

  /**
   * Construct an exception with an optional cause.
   *
   * @param cause The cause, if any
   *
   * @return An exception
   */

  public static BookBorrowExceptionFetchingBookFailed newException(
    final OptionType<Throwable> cause)
  {
    if (cause.isSome()) {
      return new BookBorrowExceptionFetchingBookFailed(
        ((Some<Throwable>) cause).get());
    } else {
      return new BookBorrowExceptionFetchingBookFailed();
    }
  }
}
