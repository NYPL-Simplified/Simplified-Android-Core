package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountSyncListenerType
{
  void onAccountSyncAuthenticationFailure(
    String message);

  void onAccountSyncBook(
    Book book);

  void onAccountSyncFailure(
    OptionType<Throwable> error,
    String message);

  void onAccountSyncSuccess();
}
