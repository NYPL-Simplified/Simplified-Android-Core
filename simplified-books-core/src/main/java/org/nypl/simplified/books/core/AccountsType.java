package org.nypl.simplified.books.core;

/**
 * The main interface to carry out operations relating to accounts.
 */

public interface AccountsType
{
  /**
   * @return <tt>true</tt> if the user is currently logged into an account.
   */

  boolean accountIsLoggedIn();

  /**
   * Start loading books, delivering results to the given <tt>listener</tt>.
   */

  void accountLoadBooks(
    AccountDataLoadListenerType listener);

  /**
   * Log in, delivering results to the given <tt>listener</tt>.
   */

  void accountLogin(
    AccountBarcode barcode,
    AccountPIN pin,
    AccountLoginListenerType listener);

  /**
   * Log out, delivering results to the given <tt>listener</tt>.
   */

  void accountLogout(
    AccountLogoutListenerType listener);

  /**
   * Sync books, delivering results to the given <tt>listener</tt>.
   */

  void accountSync(
    AccountSyncListenerType listener);
}
