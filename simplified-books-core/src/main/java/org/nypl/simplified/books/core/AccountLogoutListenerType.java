package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountLogoutListenerType
{
  void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message);

  void onAccountLogoutSuccess();
}
