package org.nypl.simplified.opds2

import org.nypl.simplified.links.Link
import java.net.URI

/**
 * An OPDS 2.0 feed.
 */

data class OPDS2Feed(

  /**
   * The URI of the feed.
   */

  val uri: URI,

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
   * The groups, if any.
   */

  val groups: List<OPDS2Group>,

  /**
   * The feed links
   */

  val links: List<Link>,

  /**
   * The feed images
   */

  val images: List<Link>,

  /**
   * The catalogs, if any.
   */

  val catalogs: List<OPDS2Catalog>

) : OPDS2ElementType
