package org.nypl.simplified.books.controller.api;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

/**
 * An exception indicating that book borrowing failed because the book requires credentials to
 * borrow and the user doesn't have any.
 */

public final class BookBorrowExceptionNoCredentials
  extends BookBorrowException
{
  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BookBorrowExceptionNoCredentials(final Throwable cause)
  {
    super(cause);
  }

  /**
   * Construct an exception.
   */

  public BookBorrowExceptionNoCredentials()
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

  public static BookBorrowExceptionNoCredentials newException(
    final OptionType<Throwable> cause)
  {
    if (cause.isSome()) {
      return new BookBorrowExceptionNoCredentials(
        ((Some<Throwable>) cause).get());
    } else {
      return new BookBorrowExceptionNoCredentials();
    }
  }
}
