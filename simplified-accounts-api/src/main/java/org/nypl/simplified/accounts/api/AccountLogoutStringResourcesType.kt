package org.nypl.simplified.accounts.api

/**
 * An interface providing localized string resources for logout processes.
 */

interface AccountLogoutStringResourcesType {

  /**
   * The device could not be deactivated because the Adobe connector raised an exception.
   */

  fun logoutDeactivatingDeviceAdobeFailed(
    errorCode: String,
    e: Throwable
  ): String

  /**
   * An unexpected exception occurred.
   */

  val logoutUnexpectedException: String

  /**
   * Posting to the device manager URI has completed.
   */

  val logoutDeviceDeactivationPostDeviceManagerFinished: String

  /**
   * Posting to the device manager URI has started.
   */

  val logoutDeviceDeactivationPostDeviceManager: String

  /**
   * The device could not be deactivated because support for DRM seems to have gone away.
   */

  val logoutDeactivatingDeviceAdobeUnsupported: String

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
