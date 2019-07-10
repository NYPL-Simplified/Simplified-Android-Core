package org.nypl.simplified.accounts.api

import org.joda.time.DateTime
import java.net.URI

/**
 * The metadata associated with the description of an account provider.
 *
 * @see AccountProviderDescriptionType
 */

data class AccountProviderDescriptionMetadata(

  /**
   * The unique identifier for the account provider.
   *
   * @see [AccountProviderType.id]
   */

  val id: URI,

  /**
   * The account title.
   *
   * @see [AccountProviderType.displayName]
   */

  val title: String,

  /**
   * The time that the description was last updated.
   */

  val updated: DateTime,

  /**
   * The links associated with the provider description.
   */

  val links: List<Link>,

  /**
   * The images associated with the provider description.
   */

  val images: List<Link>,

  /**
   * `true` if the account should be automatically added.
   *
   * @see [AccountProviderType.addAutomatically]
   */

  val isAutomatic: Boolean,

  /**
   * `true` if the library is a production library.
   */

  val isProduction: Boolean

) : Comparable<AccountProviderDescriptionMetadata> {

  /**
   * A link in a description.
   */

  data class Link(

    /**
     * The target of the link.
     */

    val href: URI,

    /**
     * The MIME type of the link content.
     */

    val type: String?,

    /**
     * `true` if the link target is templated
     */

    val templated: Boolean,

    /**
     * The relation of the link.
     */

    val relation: String?)

  override fun compareTo(other: AccountProviderDescriptionMetadata): Int =
    this.title.compareTo(other.title)

  /**
   * The logo URI, if one is available
   */

  val logoURI: URI?
    get() = this.images.find { link -> link.relation == "http://opds-spec.org/image/thumbnail" }?.href

  /**
   * The authentication document URI, if one is available
   */

  val authenticationDocumentURI: URI?
    get() = this.links.find { link -> link.type == "application/vnd.opds.authentication.v1.0+json" }?.href
}
