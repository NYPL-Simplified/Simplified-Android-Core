package org.nypl.simplified.books.core;

/**
 * The type of listeners for receiving cached credentials.
 */

public interface AccountGetCachedCredentialsListenerType
{
  /**
   * The account is not logged in.
   */

  void onAccountIsNotLoggedIn();

  /**
   * The account is logged in with the given credentials.
   *
   * @param barcode The current barcode
   * @param pin     The current PIN
   */

  void onAccountIsLoggedIn(
    AccountBarcode barcode,
    AccountPIN pin);
}
