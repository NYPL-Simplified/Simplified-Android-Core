package org.nypl.simplified.app.login

import android.content.res.Resources
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.app.R

/**
 * Strings related to logout events.
 */

class LogoutStringResources(val resources: Resources) : AccountLogoutStringResourcesType {

  override val logoutUnexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override fun logoutDeactivatingDeviceAdobeFailed(
    errorCode: String,
    e: Throwable): String {
    return this.resources.getString(R.string.logoutDeactivatingDeviceAdobeFailed, errorCode, e.javaClass.simpleName)
  }

  override val logoutDeviceDeactivationPostDeviceManagerFinished: String
    get() = this.resources.getString(R.string.logoutDeviceDeactivationPostDeviceManagerFinished)

  override val logoutDeviceDeactivationPostDeviceManager: String
    get() = this.resources.getString(R.string.logoutDeviceDeactivationPostDeviceManager)

  override val logoutDeactivatingDeviceAdobeUnsupported: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobeUnsupported)

  override val logoutDeactivatingDeviceAdobeDeactivated: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobeDeactivated)

  override val logoutDeactivatingDeviceAdobeNotActive: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobeNotActive)

  override val logoutDeactivatingDeviceAdobe: String
    get() = this.resources.getString(R.string.logoutDeactivatingDeviceAdobe)

  override val logoutClearingBookRegistryFailed: String
    get() = this.resources.getString(R.string.logoutClearingBookRegistryFailed)

  override val logoutClearingBookRegistry: String
    get() = this.resources.getString(R.string.logoutClearingBookRegistry)

  override val logoutClearingBookDatabase: String
    get() = this.resources.getString(R.string.logoutClearingBookDatabase)

  override val logoutClearingBookDatabaseFailed: String
    get() = this.resources.getString(R.string.logoutClearingBookDatabaseFailed)

  override val logoutNotLoggedIn: String
    get() = this.resources.getString(R.string.logoutNotLoggedIn)

  override val logoutStarted: String
    get() = this.resources.getString(R.string.logoutStarted)

}