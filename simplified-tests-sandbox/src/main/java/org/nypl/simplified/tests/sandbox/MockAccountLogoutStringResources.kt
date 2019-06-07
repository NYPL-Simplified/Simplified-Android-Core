package org.nypl.simplified.tests.sandbox

import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType

class MockAccountLogoutStringResources : AccountLogoutStringResourcesType {

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