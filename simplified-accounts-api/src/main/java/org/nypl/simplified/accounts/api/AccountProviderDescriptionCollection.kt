package org.nypl.simplified.accounts.api

import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.links.Link

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