package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountDataLoadListenerType
{
  void onAccountDataBookLoadFailed(
    final BookID id,
    final OptionType<Throwable> error,
    final String message);

  void onAccountDataBookLoadFinished();

  void onAccountDataBookLoadSucceeded(
    final BookID book,
    final BookSnapshot snap);

  void onAccountUnavailable();
}
