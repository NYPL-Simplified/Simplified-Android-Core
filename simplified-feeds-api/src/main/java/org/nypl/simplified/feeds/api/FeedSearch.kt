package org.nypl.simplified.feeds.api

import org.nypl.simplified.opds.core.OPDSOpenSearch1_1
import java.io.Serializable

/**
 * The type of feed searches.
 */

sealed class FeedSearch : Serializable {

  /*
   * The search should be performed locally.
   */

  object FeedSearchLocal : FeedSearch()

  /**
   * The search should be performed via an OpenSearch 1.1 REST API.
   */

  data class FeedSearchOpen1_1(
    val search: OPDSOpenSearch1_1
  ) : FeedSearch()
}
