package org.nypl.simplified.opds2

import org.joda.time.DateTime
import java.net.URI

/**
 * Metadata for an OPDS 2.0 feed.
 */

interface OPDS2MetadataType : OPDS2ElementType {

  /**
   * The unique identifier for the publication.
   */

  val identifier: URI?

  /**
   * The title of the publication.
   */

  val title: OPDS2Title

  /**
   * The subtitle of the publication.
   */

  val subtitle: OPDS2Title?

  /**
   * The time the publication was last modified.
   */

  val modified: DateTime?

  /**
   * The time the publication was published.
   */

  val published: DateTime?

  /**
   * The languages that apply to the publication.
   */

  val languages: List<String>

  /**
   * The text value used to sort the publication.
   */

  val sortAs: String?

  /**
   * The authors.
   */

  val author: List<OPDS2Contributor>
}
