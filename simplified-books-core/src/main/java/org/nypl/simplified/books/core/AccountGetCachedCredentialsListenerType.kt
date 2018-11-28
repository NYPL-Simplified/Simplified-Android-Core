package org.nypl.simplified.books.core

/**
 * The type of listeners for receiving cached credentials.
 */

interface AccountGetCachedCredentialsListenerType {

  /**
   * The account is not logged in.
   */

  fun onAccountIsNotLoggedIn()

  /**
   * The account is logged in with the given credentials.
   *
   * @param credentials The current credentials
   */

  fun onAccountIsLoggedIn(credentials: AccountCredentials)

}
