package org.nypl.simplified.profiles.controller.api

import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedBooksSelection.BOOKS_FEED_LOANED
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.Sorting.SortBy
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import java.net.URI

/**
 * The type of feed requests.
 */

data class ProfileFeedRequest(

  /**
   * The URI that will be requested.
   */

  val uri: URI,

  /**
   * The ID of the feed.
   */

  val id: String = uri.toString(),

  /**
   * The time the results were last updated.
   */

  val updated: DateTime = DateTime.now(),

  /**
   * The title of the feed results.
   */

  val title: String,

  /**
   * The active sorting facet.
   */

  val sortBy: SortBy = SortBy.SORT_BY_TITLE,

  /**
   * The title provider for facets.
   */

  val facetTitleProvider: FeedFacetPseudoTitleProviderType,

  /**
   * The search string, if any.
   */

  val search: String? = null,

  /**
   * The feed selection type.
   */

  val feedSelection: FeedBooksSelection = BOOKS_FEED_LOANED,

  /**
   * If an account ID is specified, only books on the respective account will be shown.
   */

  val filterByAccountID: AccountID? = null
)
