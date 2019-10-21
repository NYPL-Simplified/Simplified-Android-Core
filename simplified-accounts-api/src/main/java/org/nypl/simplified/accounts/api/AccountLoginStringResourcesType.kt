package org.nypl.simplified.accounts.api

/**
 * An interface providing localized string resources for login processes.
 */

interface AccountLoginStringResourcesType {

  /**
   * Device activation failed with an exception.
   */

  fun loginDeviceActivationFailed(e: Throwable): String

  /**
   * Authentication *is* required.
   */

  val loginAuthRequired: String

  /**
   * An unexpected exception occurred.
   */

  val loginUnexpectedException: String

  /**
   * Posting the device ID to the device manager has finished.
   */

  val loginDeviceActivationPostDeviceManagerDone: String

  /**
   * Posting the device ID to the device manager.
   */

  val loginDeviceActivationPostDeviceManager: String

  /**
   * Account requires DRM that is not supported by the current application.
   */

  val loginDeviceDRMNotSupported: String

  /**
   * Activated device successfully
   */

  val loginDeviceActivated: String

  /**
   * Activating device...
   */

  val loginDeviceActivationAdobe: String

  /**
   * "Checking is authentication is required..."
   */

  val loginCheckAuthRequired: String

  /**
   * The server returned some sort of fatal error.
   */

  fun loginServerError(
    status: Int,
    message: String
  ): String

  /**
   * "Authentication is not required"
   */

  val loginAuthNotRequired: String

  /**
   * A patron settings request is about to be made to the server.
   */

  val loginPatronSettingsRequest: String

  /**
   * The patron settings were retrieved and parsed correctly.
   */

  val loginPatronSettingsRequestOK: String

  /**
   * No URI was available to make the patron settings request.
   */

  val loginPatronSettingsRequestNoURI: String

  /**
   * The credentials presented to the server are not valid.
   */

  val loginPatronSettingsInvalidCredentials: String

  /**
   * A connection could not be made to the remote server.
   */

  val loginPatronSettingsConnectionFailed: String

  /**
   * Parsing a patron profile failed.
   */

  fun loginPatronSettingsRequestParseFailed(errors: List<String>): String

}
