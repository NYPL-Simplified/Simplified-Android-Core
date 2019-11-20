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
   * `true` if the feed requires network connectivity. This translates to showing an error
   * if a request is made to show the feed, but the device doesn't currently have network
   * connectivity.
   */

  abstract val requiresNetworkConnectivity: Boolean

  /**
   * The title to be displayed in the action bar for the feed.
   */

  abstract val title: String

  /**
   * `true` if the feed is comprised of search results.
   */

  abstract val isSearchResults: Boolean

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
    override val requiresNetworkConnectivity = true
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
    override val requiresNetworkConnectivity = false
    override val isSearchResults: Boolean = false
  }
}