package org.nypl.simplified.ui.catalog

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting.SortBy
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
   * The ownership of this feed.
   */

  abstract val ownership: CatalogFeedOwnership

  /**
   * Arguments that specify a remote feed. Note that feeds consisting of books bundled into the
   * application are still considered to be "remote" because they consist of data that is effectively
   * external to the application.
   */

  data class CatalogFeedArgumentsRemote(
    override val title: String,
    override val ownership: CatalogFeedOwnership.OwnedByAccount,
    val feedURI: URI,
    override val isSearchResults: Boolean
  ) : CatalogFeedArguments() {
    override val isLocallyGenerated: Boolean = false
  }

  /**
   * Arguments that specify a locally generated feed.
   */

  data class CatalogFeedArgumentsLocalBooks(
    override val title: String,
    override val ownership: CatalogFeedOwnership.CollectedFromAccounts,
    val sortBy: SortBy,
    val searchTerms: String?,
    val selection: FeedBooksSelection,
    val filterAccount: AccountID?
  ) : CatalogFeedArguments() {
    override val isSearchResults: Boolean = false
    override val isLocallyGenerated: Boolean = true
  }
}
