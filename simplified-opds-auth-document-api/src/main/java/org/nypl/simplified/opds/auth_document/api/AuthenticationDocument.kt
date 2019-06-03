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
   * Extra resources.
   *
   * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#232-links"
   */

  val links: List<AuthenticationObjectLink>)
