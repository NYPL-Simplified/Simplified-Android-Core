package org.nypl.simplified.accounts.api

import org.nypl.drm.core.AdobeVendorID
import java.net.URI

/**
 * An account provider description collection.
 */

data class AccountProviderDescriptionCollection(

  /**
   * The list of account providers.
   */

  val providers: List<AccountProviderDescriptionMetadata>,

  /**
   * The list of links associated with the collection.
   */

  val links: List<Link>,

  /**
   * The metadata associated with the collection.
   */

  val metadata: Metadata) {

  /**
   * A link.
   */

  data class Link(

    /**
     * The target of the link.
     */

    val href: URI,

    /**
     * The type of the link content.
     */

    val type: String?,

    /**
     * The relation of the link.
     */

    val relation: String?,

    /**
     * `true` if the link is templated.
     */

    val templated: Boolean = false)

  /**
   * The metadata associated with the collection.
   */

  data class Metadata(

    /**
     * The Adobe vendor ID, if one is to be used.
     */

    val adobeVendorID: AdobeVendorID?,

    /**
     * The title of the collection.
     */

    val title: String)
}