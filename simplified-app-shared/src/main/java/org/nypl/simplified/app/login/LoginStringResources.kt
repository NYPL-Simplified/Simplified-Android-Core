package org.nypl.simplified.app.login

import android.content.res.Resources
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.app.R

/**
 * Strings related to login events.
 */

class LoginStringResources(val resources: Resources) : AccountLoginStringResourcesType {

  override val loginUnexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override fun loginDeviceActivationFailed(e: Throwable): String =
    this.resources.getString(R.string.loginDeviceActivationFailed, e.localizedMessage, e.javaClass.simpleName)

  override val loginDeviceActivationPostDeviceManagerDone: String
    get() = this.resources.getString(R.string.loginDeviceActivationPostDeviceManagerDone)

  override val loginDeviceActivationPostDeviceManager: String
    get() = this.resources.getString(R.string.loginDeviceActivationPostDeviceManager)

  override val loginDeviceDRMNotSupported: String
    get() = this.resources.getString(R.string.loginDeviceDRMNotSupported)

  override val loginDeviceActivated: String
    get() = this.resources.getString(R.string.loginDeviceActivated)

  override val loginDeviceActivationAdobe: String
    get() = this.resources.getString(R.string.loginDeviceActivating)

  override val loginCheckAuthRequired: String
    get() = this.resources.getString(R.string.loginCheckingAuthenticationRequirement)

  override fun loginServerError(status: Int, message: String): String =
    this.resources.getString(R.string.loginErrorServer, status, message)

  override val loginAuthNotRequired: String
    get() = this.resources.getString(R.string.loginErrorAuthNotRequired)

  override val loginPatronSettingsRequest: String
    get() = this.resources.getString(R.string.loginPatronSettingsRequesting)

  override val loginPatronSettingsRequestOK: String
    get() = this.resources.getString(R.string.loginPatronSettingsOK)

  override val loginPatronSettingsRequestNoURI: String
    get() = this.resources.getString(R.string.loginErrorNoPatronURI)

  override val loginPatronSettingsInvalidCredentials: String
    get() = this.resources.getString(R.string.loginErrorInvalidCredentials)

  override val loginPatronSettingsConnectionFailed: String
    get() = this.resources.getString(R.string.loginErrorConnectionFailed)

  override fun loginPatronSettingsRequestParseFailed(errors: List<String>): String =
    this.resources.getString(R.string.loginErrorPatronSettingsParseError)
}
