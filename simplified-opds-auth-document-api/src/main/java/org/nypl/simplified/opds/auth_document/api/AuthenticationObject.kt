package org.nypl.simplified.opds.auth_document.api

import com.google.common.base.Preconditions
import org.nypl.simplified.links.Link
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
   * A description of the authentication object (such as "Library Barcode")
   */

  val description: String = "",

  /**
   * Optional labels for the authentication object.
   */

  val labels: Map<String, String> = mapOf(),

  /**
   * Optional input extensions.
   */

  val inputs: Map<String, AuthenticationObjectNYPLInput> = mapOf(),

  /**
   * Links for the object.
   */

  val links: List<Link> = listOf()
) {

  init {
    Preconditions.checkArgument(
      this.inputs.keys.all { field -> field.all { c -> c.isUpperCase() } },
      "Input keys ${this.inputs.keys} must all be uppercase"
    )
    Preconditions.checkArgument(
      this.labels.keys.all { field -> field.all { c -> c.isUpperCase() } },
      "Labels keys ${this.labels.keys} must all be uppercase"
    )
  }

  companion object {

    /**
     * Alternate label for a login.
     *
     * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#311-labels"
     */

    const val LABEL_LOGIN = "LOGIN"

    /**
     * Alternate label for a password.
     *
     * @see "https://drafts.opds.io/authentication-for-opds-1.0.html#311-labels"
     */

    const val LABEL_PASSWORD = "PASSWORD"
  }
}
