package org.nypl.simplified.ui.catalog

import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FacetType
import java.io.Serializable
import java.net.URI

/**
 * The type of arguments used to fetch and/or display feeds.
 */

sealed class CatalogFeedArguments : Serializable {

  /**
   * The title to be displayed in the action bar for the feed.
   */

  abstract val title: String

  /**
   * `true` if the feed is comprised of search results.
   */

  abstract val isSearchResults: Boolean

  /**
   * `true` if the feed is a locally generated feed (such as the My Books or Holds view).
   */

  abstract val isLocallyGenerated: Boolean

  /**
   * Arguments that specify a remote feed. Note that feeds consisting of books bundled into the
   * application are still considered to be "remote" because they consist of data that is effectively
   * external to the application.
   */

  data class CatalogFeedArgumentsRemote(
    override val title: String,
    val feedURI: URI,
    override val isSearchResults: Boolean
  ) : CatalogFeedArguments() {
    override val isLocallyGenerated: Boolean = false
  }

  /**
   * Arguments that specify whatever is the default remote feed for the account that is current
   * at the time the loading is invoked.
   */

  data class CatalogFeedArgumentsRemoteAccountDefault(
    override val title: String
  ) : CatalogFeedArguments() {
    override val isSearchResults: Boolean = false
    override val isLocallyGenerated: Boolean = false
  }

  /**
   * Arguments that specify a locally generated feed.
   */

  data class CatalogFeedArgumentsLocalBooks(
    override val title: String,
    val facetType: FacetType,
    val searchTerms: String?,
    val selection: FeedBooksSelection
  ) : CatalogFeedArguments() {
    override val isSearchResults: Boolean = false
    override val isLocallyGenerated: Boolean = true
  }
}
