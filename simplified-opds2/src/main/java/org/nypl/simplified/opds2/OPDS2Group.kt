package org.nypl.simplified.opds2

import org.nypl.simplified.links.Link

/**
 * An OPDS 2.0 group.
 */

data class OPDS2Group(

  /**
   * The feed metadata.
   */

  val metadata: OPDS2Metadata,

  /**
   * The navigation section, if any.
   */

  val navigation: OPDS2Navigation?,

  /**
   * The publications, if any.
   */

  val publications: List<OPDS2Publication>,

  /**
   * The feed links
   */

  val links: List<Link>

) : OPDS2ElementType
