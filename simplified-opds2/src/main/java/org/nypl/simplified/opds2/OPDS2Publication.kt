package org.nypl.simplified.opds2

import org.nypl.simplified.links.Link

/**
 * An OPDS 2.0 publication.
 */

data class OPDS2Publication(

  /**
   * Metadata for a publication.
   */

  val metadata: OPDS2Metadata,

  /**
   * Links for the publication.
   */

  val links: List<Link> = listOf(),

  /**
   * The reading order for the publication.
   */

  val readingOrder: List<Link> = listOf(),

  /**
   * The resources for the publication.
   */

  val resources: List<Link> = listOf(),

  /**
   * The table of contents for the publication.
   */

  val tableOfContents: List<Link> = listOf(),

  /**
   * The list of images associated with the publication.
   */

  val images: List<Link> = listOf()

) : OPDS2ElementType
