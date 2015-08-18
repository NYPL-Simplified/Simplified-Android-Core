package org.nypl.simplified.app;

import com.io7m.jfunctional.OptionType;

/**
 * A listener that receives the results of login attempts.
 */

public interface LoginControllerListenerType
{
  /**
   * The user cancelled the login.
   */

  void onLoginAborted();

  /**
   * The user failed to log in, typically due to a server error.
   *
   * @param error   The exception raised, if any
   * @param message The error message
   */

  void onLoginFailure(
    OptionType<Throwable> error,
    String message);

  /**
   * The user successfully logged in.
   */

  void onLoginSuccess();
}
