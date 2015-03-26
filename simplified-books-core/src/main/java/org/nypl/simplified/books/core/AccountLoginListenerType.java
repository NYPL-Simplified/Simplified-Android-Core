package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountLoginListenerType
{
  void onAccountLoginFailure(
    OptionType<Throwable> error,
    String message);

  void onAccountLoginSuccess(
    AccountBarcode barcode,
    AccountPIN pin);
}
