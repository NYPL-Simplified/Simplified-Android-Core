package org.nypl.simplified.ui.accounts.saml20

/**
 * Constants related to the SAML 2.0 implementation.
 */

object AccountSAML20 {

  /**
   * The callback URI used to allow a SAML web page to effectively call back into the application.
   * In practice, the web view will intercept requests made to this URI.
   */

  val callbackURI = "simplified-saml20://authenticated"
}
