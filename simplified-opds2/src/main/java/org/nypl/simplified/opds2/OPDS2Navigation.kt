package org.nypl.simplified.opds2

import org.nypl.simplified.links.Link

/**
 * The OPDS 2.0 navigation section.
 */

data class OPDS2Navigation(

  /**
   * The set of navigation links.
   */

  val links: List<Link>

) : OPDS2ElementType
