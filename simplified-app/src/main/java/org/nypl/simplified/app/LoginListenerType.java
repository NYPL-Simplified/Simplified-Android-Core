package org.nypl.simplified.app;

import com.io7m.jfunctional.OptionType;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountPIN;

/**
 * A listener that receives the results of login attempts.
 */

public interface LoginListenerType
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
   *
   * @param user     The current user name
   * @param password The current password
   */

  void onLoginSuccess(
    AccountBarcode user,
    AccountPIN password);
}
