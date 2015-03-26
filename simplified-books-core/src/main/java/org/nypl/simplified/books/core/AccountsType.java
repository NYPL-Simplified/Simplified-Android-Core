package org.nypl.simplified.books.core;

public interface AccountsType
{
  boolean accountIsLoggedIn();

  void accountLoadBooks(
    AccountDataLoadListenerType listener);

  void accountLogin(
    AccountBarcode barcode,
    AccountPIN pin,
    AccountLoginListenerType listener);

  void accountLogout(
    AccountLogoutListenerType listener);

  void accountSync(
    AccountSyncListenerType listener);
}
