package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface BookBorrowListenerType
{
  void onBookBorrowFailure(
    BookID id,
    OptionType<Throwable> e);

  void onBookBorrowSuccess(
    BookID id);
}
