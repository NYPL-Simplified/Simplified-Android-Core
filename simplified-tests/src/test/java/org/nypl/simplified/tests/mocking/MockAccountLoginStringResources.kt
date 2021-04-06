package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType

class MockAccountLoginStringResources : AccountLoginStringResourcesType {

  override val loginAuthRequired: String
    get() = "loginAuthRequired"

  override val loginUnexpectedException: String
    get() = "loginUnexpectedException"

  override val loginDeviceActivationPostDeviceManagerDone: String
    get() = "loginDeviceActivationPostDeviceManagerDone"

  override val loginDeviceActivationPostDeviceManager: String
    get() = "loginDeviceActivationPostDeviceManager"

  override fun loginDeviceActivationFailed(e: Throwable): String {
    return "loginDeviceActivationFailed"
  }

  override val loginDeviceDRMNotSupported: String
    get() = "loginDeviceDRMNotSupported"

  override val loginDeviceActivated: String
    get() = "loginDeviceActivated"

  override val loginDeviceActivationAdobe: String
    get() = "loginDeviceActivationAdobe"

  override val loginPatronSettingsConnectionFailed: String
    get() = "loginPatronSettingsConnectionFailed"

  override val loginPatronSettingsInvalidCredentials: String
    get() = "loginPatronSettingsInvalidCredentials"

  override fun loginServerError(status: Int, message: String): String {
    return "loginServerError $status $message"
  }

  override val loginCheckAuthRequired: String
    get() = "loginCheckAuthRequired"

  override val loginAuthNotRequired: String
    get() = "loginAuthNotRequired"

  override val loginPatronSettingsRequest: String
    get() = "loginPatronSettingsRequest"

  override val loginPatronSettingsRequestOK: String
    get() = "loginPatronSettingsRequestOK"

  override val loginPatronSettingsRequestNoURI: String
    get() = "loginPatronSettingsRequestNoURI"

  override fun loginPatronSettingsRequestParseFailed(errors: List<String>): String =
    "loginPatronSettingsRequestParseFailed"
}
