package org.nypl.simplified.books.core;

public interface AccountsType
{
  void accountLoadBooks(
    AccountDataLoadListenerType listener);

  void accountLogin(
    AccountBarcode barcode,
    AccountPINListenerType pin_listener,
    AccountLoginListenerType listener);

  void accountLogout(
    AccountLogoutListenerType listener);

  void accountSync(
    AccountPINListenerType pin_listener,
    AccountSyncListenerType listener);
}
