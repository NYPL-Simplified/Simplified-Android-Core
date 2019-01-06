package org.nypl.simplified.app.login;

import com.io7m.jfunctional.OptionType;

public interface LoginFailedType {

  /**
   * The user failed to log in, typically due to a server error.
   *
   * @param error   The exception raised, if any
   * @param message The error message
   */

  void onLoginFailed(
    OptionType<Exception> error,
    String message);

}
