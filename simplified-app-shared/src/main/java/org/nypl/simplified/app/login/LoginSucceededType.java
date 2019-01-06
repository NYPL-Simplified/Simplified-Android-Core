package org.nypl.simplified.app.login;

import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;

public interface LoginSucceededType {

  /**
   * The user successfully logged in.
   *
   * @param creds The account credentials
   */

  void onLoginSucceeded(
    AccountAuthenticationCredentials creds);

}
