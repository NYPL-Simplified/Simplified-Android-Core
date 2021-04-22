package org.nypl.simplified.opds2

import org.joda.time.DateTime
import java.net.URI

data class OPDS2CatalogMetadata(

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

  override val author: List<OPDS2Contributor>,

  /**
   * The Adobe vendor ID
   */

  val adobeVendorId: String?,

  /**
   * `true` if the catalog is in production.
   */

  val isProduction: Boolean,

  /**
   * `true` if the catalog should be automatically added to new profiles.
   */

  val isAutomatic: Boolean

) : Comparable<OPDS2CatalogMetadata>, OPDS2MetadataType {

  override fun compareTo(other: OPDS2CatalogMetadata): Int =
    (this.sortAs ?: this.title.title).compareTo(other.sortAs ?: other.title.title)
}
