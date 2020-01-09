package org.nypl.simplified.feeds.api

import org.nypl.simplified.opds.core.OPDSFacet
import java.io.Serializable

sealed class FeedFacet : Serializable {

  abstract val title: String

  abstract val isActive: Boolean

  /**
   * A facet taken from an actual OPDS feed.
   */

  data class FeedFacetOPDS(val opdsFacet: OPDSFacet) : FeedFacet() {

    override val title: String = this.opdsFacet.title
    override val isActive: Boolean = this.opdsFacet.isActive

    companion object {
      const val ENTRYPOINT_FACET_GROUP_TYPE = "http://librarysimplified.org/terms/rel/entrypoint"
    }
  }

  /**
   * A pseudo-facet.
   *
   * This is used to provide facets for locally generated feeds.
   */

  data class FeedFacetPseudo(
    override val title: String,
    override val isActive: Boolean,
    val type: FacetType
  ) : FeedFacet() {

    /**
     * The type of facets.
     */

    enum class FacetType {
      /**
       * Sort the feed in question by author.
       */

      SORT_BY_AUTHOR,

      /**
       * Sort the feed in question by book title.
       */

      SORT_BY_TITLE
    }
  }
}
