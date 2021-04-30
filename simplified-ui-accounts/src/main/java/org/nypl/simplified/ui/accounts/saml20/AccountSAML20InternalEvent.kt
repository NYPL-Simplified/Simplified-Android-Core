package org.nypl.simplified.ui.accounts.saml20

import org.nypl.simplified.accounts.api.AccountCookie

/**
 * Events raised during the SAML login process.
 */

sealed class AccountSAML20InternalEvent {

  /**
   * The web view client is ready for use. The login page should not be loaded until this event has
   * fired.
   */

  class WebViewClientReady() : AccountSAML20InternalEvent()

  /**
   * The process failed.
   */

  data class Failed(
    val message: String
  ) : AccountSAML20InternalEvent()

  /**
   * An access token was obtained.
   */

  data class AccessTokenObtained(
    val token: String,
    val patronInfo: String,
    val cookies: List<AccountCookie>
  ) : AccountSAML20InternalEvent()
}
