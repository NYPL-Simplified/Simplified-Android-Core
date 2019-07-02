package org.nypl.simplified.accounts.api

import org.joda.time.DateTime
import java.net.URI

/**
 * A description of an account provider. Descriptions are _resolved_ to produce [AccountProviderType]
 * values.
 */

interface AccountProviderDescriptionType : Comparable<AccountProviderDescriptionType> {

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

  /**
   * The unique identifier for the account provider.
   *
   * @see [AccountProviderType.id]
   */

  val id: URI

  /**
   * The account title.
   *
   * @see [AccountProviderType.displayName]
   */

  val title: String

  /**
   * The time that the description was last updated.
   */

  val updated: DateTime

  /**
   * The links associated with the provider description.
   */

  val links: List<Link>

  /**
   * The images associated with the provider description.
   */

  val images: List<Link>

  /**
   * `true` if the account should be automatically added.
   *
   * @see [AccountProviderType.addAutomatically]
   */

  val isAutomatic: Boolean

  /**
   * `true` if the library is a production library.
   */

  val isProduction: Boolean

  /**
   * Resolve the description into a full account provider. The given `onProgress` function
   * will be called repeatedly during the resolution process to report on the status of the
   * resolution.
   */

  fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult

  override fun compareTo(other: AccountProviderDescriptionType): Int =
    this.title.compareTo(other.title)

  /**
   * The logo URI, if one is available
   */

  val logoURI: URI?
    get() = this.images.find { link -> link.relation == "http://opds-spec.org/image/thumbnail" }?.href
}
