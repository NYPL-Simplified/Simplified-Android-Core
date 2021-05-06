package org.nypl.simplified.opds2

import org.nypl.simplified.links.Link

/**
 * A description of an OPDS 2.0 catalog.
 */

data class OPDS2Catalog(

  /**
   * The catalog metadata.
   */

  val metadata: OPDS2CatalogMetadata,

  /**
   * The catalog links
   */

  val links: List<Link>,

  /**
   * The catalog images
   */

  val images: List<Link>

) : OPDS2ElementType
