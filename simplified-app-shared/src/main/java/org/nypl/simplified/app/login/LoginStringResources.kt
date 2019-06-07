package org.nypl.simplified.app.login

import android.content.res.Resources
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.app.R

/**
 * Strings related to login events.
 */

class LoginStringResources(val resources: Resources) : AccountLoginStringResourcesType {

  override val loginDeviceActivated: String
    get() = this.resources.getString(R.string.loginDeviceActivated)

  override val loginDeviceActivation: String
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