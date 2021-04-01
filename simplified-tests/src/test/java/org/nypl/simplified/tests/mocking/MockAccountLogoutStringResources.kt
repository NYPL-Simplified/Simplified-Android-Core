package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType

class MockAccountLogoutStringResources : AccountLogoutStringResourcesType {

  override val logoutUnexpectedException: String
    get() = "logoutUnexpectedException"

  override fun logoutDeactivatingDeviceAdobeFailed(errorCode: String, e: Throwable): String {
    return "logoutDeactivatingDeviceAdobeFailed"
  }

  override val logoutDeviceDeactivationPostDeviceManagerFinished: String
    get() = "logoutDeviceDeactivationPostDeviceManagerFinished"

  override val logoutDeviceDeactivationPostDeviceManager: String
    get() = "logoutDeviceDeactivationPostDeviceManager"

  override val logoutDeactivatingDeviceAdobeUnsupported: String
    get() = "logoutDeactivatingDeviceAdobeUnsupported"

  override val logoutDeactivatingDeviceAdobeDeactivated: String
    get() = "logoutDeactivatingDeviceAdobeDeactivated"

  override val logoutDeactivatingDeviceAdobeNotActive: String
    get() = "logoutDeactivatingDeviceAdobeNotActive"

  override val logoutDeactivatingDeviceAdobe: String
    get() = "logoutDeactivatingDeviceAdobe"

  override val logoutClearingBookRegistryFailed: String
    get() = "logoutClearingBookRegistryFailed"

  override val logoutClearingBookRegistry: String
    get() = "logoutClearingBookRegistry"

  override val logoutClearingBookDatabase: String
    get() = "logoutClearingBookDatabase"

  override val logoutClearingBookDatabaseFailed: String
    get() = "logoutClearingBookDatabaseFailed"

  override val logoutNotLoggedIn: String
    get() = "logoutNotLoggedIn"

  override val logoutStarted: String
    get() = "logoutStarted"
}
