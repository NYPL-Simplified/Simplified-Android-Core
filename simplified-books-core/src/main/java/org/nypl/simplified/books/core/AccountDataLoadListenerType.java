package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountDataLoadListenerType
{
  void onAccountDataBookLoadFailed(
    final BookID id,
    final OptionType<Throwable> error,
    final String message);

  void onAccountDataBookLoadSucceeded(
    final Book book);

  void onAccountUnavailable();
}
