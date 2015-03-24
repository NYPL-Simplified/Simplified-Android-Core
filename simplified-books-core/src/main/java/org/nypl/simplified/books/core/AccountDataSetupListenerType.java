package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountDataSetupListenerType
{
  void onAccountDataSetupFailure(
    final OptionType<Throwable> error,
    final String message);

  void onAccountDataSetupSuccess();
}
