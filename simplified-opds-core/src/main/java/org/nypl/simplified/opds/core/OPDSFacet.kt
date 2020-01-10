package org.nypl.simplified.opds.core

import com.io7m.jfunctional.OptionType
import java.io.Serializable
import java.net.URI

/**
 * An OPDS facet.
 *
 * See http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Facets.
 */

data class OPDSFacet(

  /**
   * `true` if the facet is active
   */

  val isActive: Boolean,

  /**
   * The URI
   */

  val uri: URI,

  /**
   * The group
   */

  val group: String,

  /**
   * The title
   */

  val title: String,

  /**
   * The group type
   */

  val groupType: OptionType<String>
) : Serializable {

  companion object {
    private const val serialVersionUID = 1L
  }
}
