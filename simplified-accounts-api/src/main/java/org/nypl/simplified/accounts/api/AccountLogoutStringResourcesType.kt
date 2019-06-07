package org.nypl.simplified.accounts.api

/**
 * An interface providing localized string resources for logout processes.
 */

interface AccountLogoutStringResourcesType {

  /**
   * The device was deactivated successfully.
   */

  val logoutDeactivatingDeviceAdobeDeactivated: String

  /**
   * The device didn't appear to be active and so didn't need deactivating.
   */

  val logoutDeactivatingDeviceAdobeNotActive: String

  /**
   * Deactivating a device via the Adobe library.
   */

  val logoutDeactivatingDeviceAdobe: String

  /**
   * Clearing the book registry failed.
   */

  val logoutClearingBookRegistryFailed: String

  /**
   * Clearing the book registry succeeded.
   */

  val logoutClearingBookRegistry: String

  /**
   * Clearing the book database started.
   */

  val logoutClearingBookDatabase: String

  /**
   * Clearing the book database failed.
   */

  val logoutClearingBookDatabaseFailed: String

  /**
   * The account does not appear to be logged in.
   */

  val logoutNotLoggedIn: String

  /**
   * Logging out has started.
   */

  val logoutStarted: String

}
