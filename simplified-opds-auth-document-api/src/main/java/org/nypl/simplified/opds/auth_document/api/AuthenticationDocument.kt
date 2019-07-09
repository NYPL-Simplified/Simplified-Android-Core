package org.nypl.simplified.opds.auth_document.api

import java.net.URI

/**
 * An authentication directory.
 *
 * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#2-authentication-document"
 */

data class AuthenticationDocument(

  /**
   * Unique identifier for the Catalog provider and canonical location for the Authentication Document.
   */

  val id: URI,

  /**
   * Title of the Catalog being accessed.
   */

  val title: String,

  /**
   * The name of the main color used for the application (such as `red`).
   */

  val mainColor: String,

  /**
   * A description of the service being displayed to the user.
   */

  val description: String?,

  /**
   * A list of supported Authentication Flows
   *
   * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#3-authentication-flows"
   */

  val authentication: List<AuthenticationObject>,

  /**
   * A list of the feature flags (an NYPL extension)
   *
   * @see "https://github.com/NYPL-Simplified/Simplified/wiki/Authentication-For-OPDS-Extensions#feature-flags"
   */

  val features: AuthenticationObjectNYPLFeatures,

  /**
   * Extra resources.
   *
   * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#232-links"
   */

  val links: List<AuthenticationObjectLink>) {

  val loansURI: URI? =
    this.links.find { link -> link.rel == "http://opds-spec.org/shelf" }?.href

  val cardCreatorURI: URI? =
    this.links.find { link -> link.rel == "register" }?.href

  val startURI: URI? =
    this.links.find { link -> link.rel == "start" }?.href

  val logoURI : URI? =
    this.links.find { link -> link.rel == "logo" }?.href

  val patronSettingsURI: URI? =
    this.links.find { link -> link.rel == "http://librarysimplified.org/terms/rel/user-profile" }?.href

  val eulaURI : URI? =
    this.links.find { link -> link.rel == "terms-of-service" }?.href

  val privacyPolicyURI : URI? =
    this.links.find { link -> link.rel == "privacy-policy" }?.href

  val licenseURI : URI? =
    this.links.find { link -> link.rel == "license" }?.href

  val supportURI : URI? =
    this.links.find { link -> link.rel == "help" }?.href
}
