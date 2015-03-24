package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

public interface AccountPINListenerType
{
  OptionType<AccountPIN> onAccountPINRejected();

  OptionType<AccountPIN> onAccountPINRequested();
}
