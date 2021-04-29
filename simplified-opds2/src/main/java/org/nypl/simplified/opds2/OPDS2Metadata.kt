package org.nypl.simplified.opds2

import org.joda.time.DateTime
import java.net.URI

/**
 * Metadata for an OPDS 2.0 feed.
 */

data class OPDS2Metadata(

  /**
   * The unique identifier for the publication.
   */

  override val identifier: URI?,

  /**
   * The title of the publication.
   */

  override val title: OPDS2Title,

  /**
   * The subtitle of the publication.
   */

  override val subtitle: OPDS2Title?,

  /**
   * The time the publication was last modified.
   */

  override val modified: DateTime?,

  /**
   * The time the publication was published.
   */

  override val published: DateTime?,

  /**
   * The languages that apply to the publication.
   */

  override val languages: List<String>,

  /**
   * The text value used to sort the publication.
   */

  override val sortAs: String?,

  /**
   * The authors.
   */

  override val author: List<OPDS2Contributor>
) : Comparable<OPDS2Metadata>, OPDS2MetadataType {
  override fun compareTo(other: OPDS2Metadata): Int =
    (this.sortAs ?: this.title.title).compareTo(other.sortAs ?: other.title.title)
}
