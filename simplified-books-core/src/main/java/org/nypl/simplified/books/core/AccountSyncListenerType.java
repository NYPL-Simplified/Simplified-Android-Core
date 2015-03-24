package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountSyncListenerType
{
  void onAccountSyncBook(
    final Book book);

  void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message);

  void onAccountSyncSuccess();
}
