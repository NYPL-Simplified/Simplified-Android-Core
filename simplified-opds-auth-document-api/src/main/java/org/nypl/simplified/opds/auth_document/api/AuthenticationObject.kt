package org.nypl.simplified.opds.auth_document.api

import java.net.URI

/**
 * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#31-authentication-object"
 */

data class AuthenticationObject(

  /**
   * A URI that identifies the nature of an Authentication Flow.
   */

  val type: URI,

  /**
   * Optional labels for the authentication object.
   */

  val labels: Map<String, String> = mapOf(),

  /**
   * Links for the object.
   */

  val links: List<AuthenticationObjectLink> = listOf()) {

  companion object {

    /**
     * Alternate label for a login.
     *
     * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#311-labels"
     */

    const val LABEL_LOGIN = "login"

    /**
     * Alternate label for a password.
     *
     * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#311-labels"
     */

    const val LABEL_PASSWORD = "password"

  }

}