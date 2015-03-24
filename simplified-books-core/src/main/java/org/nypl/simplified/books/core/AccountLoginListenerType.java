package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountLoginListenerType
{
  void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message);

  void onAccountLoginSuccess(
    final AccountBarcode barcode);
}
