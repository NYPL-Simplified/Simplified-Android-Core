package org.nypl.simplified.tests

import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType

class MockAccountLoginStringResources : AccountLoginStringResourcesType {

  override val loginDeviceActivated: String
    get() = "loginDeviceActivated"

  override val loginDeviceActivation: String
    get() = "loginDeviceActivation"

  override val loginPatronSettingsConnectionFailed: String
    get() = "loginPatronSettingsConnectionFailed"

  override val loginPatronSettingsInvalidCredentials: String
    get() = "loginPatronSettingsInvalidCredentials"

  override fun loginServerError(status: Int, message: String): String {
    return "loginServerError ${status} ${message}"
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