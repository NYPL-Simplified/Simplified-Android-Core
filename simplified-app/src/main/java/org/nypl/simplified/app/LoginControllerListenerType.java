package org.nypl.simplified.app;

import com.io7m.jfunctional.OptionType;

public interface LoginControllerListenerType
{
  void onLoginSuccess();

  void onLoginFailure(
    OptionType<Throwable> error,
    String message);

  void onLoginAborted();
}
