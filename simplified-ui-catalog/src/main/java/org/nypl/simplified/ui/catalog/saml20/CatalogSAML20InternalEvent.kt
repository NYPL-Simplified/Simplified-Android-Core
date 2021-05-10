package org.nypl.simplified.ui.catalog.saml20

/**
 * Events raised during the SAML login process.
 */

sealed class CatalogSAML20InternalEvent {

  /**
   * The web view client is ready for use. The login page should not be loaded until this event has
   * fired.
   */

  class WebViewClientReady() : CatalogSAML20InternalEvent()

  /**
   * The login succeeded.
   */

  class Succeeded() : CatalogSAML20InternalEvent()
}
