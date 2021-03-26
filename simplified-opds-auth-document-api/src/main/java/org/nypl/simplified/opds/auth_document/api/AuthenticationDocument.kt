package org.nypl.simplified.opds.auth_document.api

import org.nypl.simplified.announcements.Announcement
import org.nypl.simplified.links.Link
import java.net.URI

/**
 * An authentication document.
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

  val links: List<Link>,

  /**
   * Announcements provided by the library.
   *
   * @see "https://github.com/NYPL-Simplified/Simplified/wiki/Authentication-For-OPDS-Extensions#sitewide-announcements"
   */

  val announcements: List<Announcement>
) {

  val loansURI: URI? =
    this.links.find { link -> link.relation == "http://opds-spec.org/shelf" }?.hrefURI

  val cardCreatorURI: URI? =
    this.links.find { link -> link.relation == "register" }?.hrefURI

  val startURI: URI? =
    this.links.find { link -> link.relation == "start" }?.hrefURI

  val logoURI: URI? =
    this.links.find { link -> link.relation == "logo" }?.hrefURI

  val patronSettingsURI: URI? =
    this.links.find { link -> link.relation == "http://librarysimplified.org/terms/rel/user-profile" }?.hrefURI

  val eulaURI: URI? =
    this.links.find { link -> link.relation == "terms-of-service" }?.hrefURI

  val privacyPolicyURI: URI? =
    this.links.find { link -> link.relation == "privacy-policy" }?.hrefURI

  val licenseURI: URI? =
    this.links.find { link -> link.relation == "license" }?.hrefURI

  val supportURI: URI? =
    this.links.find { link -> link.relation == "help" }?.hrefURI
}
