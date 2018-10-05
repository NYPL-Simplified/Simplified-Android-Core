package org.nypl.simplified.books.core;

import com.google.common.util.concurrent.FluentFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.opds.core.DRMLicensor;

/**
 * The main interface to carry out operations relating to accounts.
 */

public interface AccountsControllerType
{
  /**
   * @return {@code true} if the user is currently logged into an account.
   */

  boolean accountIsLoggedIn();

  /**
   * Get login details delivering them to the given listener immediately.
   *
   * @param listener The listener
   */

  void accountGetCachedLoginDetails(
    AccountGetCachedCredentialsListenerType listener);

  /**
   * Start loading books, delivering results to the given {@code listener}.
   *
   * @param listener The listener
   * @param needs_auch login required
   */

  FluentFuture<Unit> accountLoadBooks(
    AccountDataLoadListenerType listener,
    boolean needs_auch);

  /**
   * Log in, delivering results to the given {@code listener}.
   *
   * @param credentials The account credentials
   * @param listener    The listener
   */

  FluentFuture<Unit> accountLogin(
    AccountCredentials credentials,
    AccountLoginListenerType listener);

  /**
   * Log out, delivering results to the given {@code listener}.
   *
   * @param credentials account credentials
   * @param listener The listener
   * @param sync_listener   Account sync listener
   * @param device_listener device activation listener
   */

  FluentFuture<Unit> accountLogout(
    AccountCredentials credentials,
    AccountLogoutListenerType listener,
    AccountSyncListenerType sync_listener,
    DeviceActivationListenerType device_listener);

  /**
   * Sync books, delivering results to the given {@code listener}.
   *
   * @param listener The listener
   * @param device_listener  device activation listener
   * @param needs_authentication true if the account needs authentication
   */

  FluentFuture<Unit> accountSync(
    AccountSyncListenerType listener,
    DeviceActivationListenerType device_listener,
    boolean needs_authentication);

  /**
   * Activate the device with the currently logged in account (if you are logged in).
   * @param licensor         drm licener
   * @param device_listener device activation listener
   */
  void accountActivateDeviceAndFulfillBooks(
    OptionType<DRMLicensor> licensor,
    DeviceActivationListenerType device_listener);

  /**
   * Activate the device with the currently logged in account (if you are logged in).
   * @param device_listener  device activation listener
   */
  void accountActivateDevice(
    DeviceActivationListenerType device_listener);

  /**
   *  fulfill all existing books which were download before
   */
  void fulfillExistingBooks();

  /**
   * Deactivate the device with the currently logged in account (if you are logged in).
   */
  void accountDeActivateDevice();

  /**
   * determine is device is active with the currently logged account.
   * @return if device is active
   */
  boolean accountIsDeviceActive();

  /**
   *
   */
  void accountRemoveCredentials();

  /**
   * @param in_book_id book id to be fulfilled
   * @param licensor licensor data
   * @param listener   account activation  listener
   */
  void accountActivateDeviceAndFulFillBook(
    BookID in_book_id,
    OptionType<DRMLicensor> licensor,
    DeviceActivationListenerType listener);



}
