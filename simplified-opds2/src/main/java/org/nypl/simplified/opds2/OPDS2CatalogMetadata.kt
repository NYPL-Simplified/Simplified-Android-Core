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
   * `true` if the catalog is in production.
   */

  val isProduction: Boolean,

  /**
   * `true` if the catalog should be automatically added to new profiles.
   */

  val isAutomatic: Boolean,

  /**
   * Library Simplified catalogs use a "location" property to express library service areas.
   *
   * @see "https://github.com/NYPL-Simplified/Simplified/wiki/LibraryRegistryPublicAPI#the-location-property"
   */

  val location: String?,

  /**
   * Library Simplified catalogs use a "distance" property to express the distance between the user and a library.
   *
   * @see "https://github.com/NYPL-Simplified/Simplified/wiki/LibraryRegistryPublicAPI#the-distance-property"
   */

  val distance: String?,

  /**
   * Library Simplified catalogs use a "library_type" property to describe library services.
   *
   * @see "https://github.com/NYPL-Simplified/Simplified/wiki/LibraryRegistryPublicAPI#the-library_type-property"
   */

  val libraryType: String?

) : Comparable<OPDS2CatalogMetadata>, OPDS2MetadataType {

  override fun compareTo(other: OPDS2CatalogMetadata): Int =
    (this.sortAs ?: this.title.title).compareTo(other.sortAs ?: other.title.title)
}
