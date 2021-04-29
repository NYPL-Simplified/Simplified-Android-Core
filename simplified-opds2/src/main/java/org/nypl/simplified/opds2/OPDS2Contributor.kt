package org.nypl.simplified.opds2

import org.nypl.simplified.links.Link
import java.net.URI

/**
 * A contributor.
 */

data class OPDS2Contributor(

  /**
   * The name of the contributor.
   */

  val name: OPDS2Name,

  /**
   * The identifier for the contributor.
   */

  val identifier: URI? = null,

  /**
   * The string used to sort the contributor name.
   */

  val sortAs: String = name.name,

  /**
   * The contributor links.
   */

  val links: List<Link> = listOf()
) : Comparable<OPDS2Contributor> {

  override fun compareTo(other: OPDS2Contributor): Int {
    return this.sortAs.compareTo(other.sortAs)
  }
}
