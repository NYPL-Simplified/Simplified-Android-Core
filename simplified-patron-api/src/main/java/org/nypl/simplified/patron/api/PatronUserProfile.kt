package org.nypl.simplified.patron.api

import org.nypl.simplified.links.Link
import java.net.URI

/**
 * Patron user profile.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/User-Profile-Management-Protocol"
 */

data class PatronUserProfile(
  val settings: PatronSettings,
  val drm: List<PatronDRM>,
  val links: List<Link>,
  val authorization: PatronAuthorization?
) {

  /**
   * The annotations URI provided in the user profile, if any.
   */

  val annotationsURI: URI? =
    this.links.find { link -> link.relation == "http://www.w3.org/ns/oa#annotationService" }
      ?.hrefURI
}
