package org.nypl.simplified.ui.catalog

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedSearch

/**
 * The state of a given feed.
 */

sealed class CatalogFeedState {

  /**
   * The arguments used to produce the feed
   */

  abstract val arguments: CatalogFeedArguments

  /**
   * The search definition associated with the feed, if any
   */

  abstract val search: FeedSearch?

  /**
   * The title of the feed.
   */

  abstract val title: String

  /**
   * The feed requires an age gate check.
   */

  data class CatalogFeedAgeGate(
    override val arguments: CatalogFeedArguments
  ) : CatalogFeedState() {
    override val title: String = ""
    override val search: FeedSearch? = null
  }

  /**
   * The feed is currently loading.
   */

  data class CatalogFeedLoading(
    override val arguments: CatalogFeedArguments,
  ) : CatalogFeedState() {
    override val title: String = ""
    override val search: FeedSearch? = null
  }

  /**
   * Loading a feed failed.
   */

  data class CatalogFeedLoadFailed(
    override val arguments: CatalogFeedArguments,
    val failure: FeedLoaderResult.FeedLoaderFailure
  ) : CatalogFeedState() {
    override val title: String = ""
    override val search: FeedSearch? = null
  }

  sealed class CatalogFeedLoaded : CatalogFeedState() {

    /**
     * A feed was loaded and it turned out to be a feed with groups. These will be
     * rendered as horizontal scrolling lanes.
     */

    data class CatalogFeedWithGroups(
      override val arguments: CatalogFeedArguments,
      val feed: Feed.FeedWithGroups
    ) : CatalogFeedLoaded() {
      override val title: String
        get() = this.feed.feedTitle
      override val search: FeedSearch? =
        this.feed.feedSearch
    }

    /**
     * A feed was loaded without groups. The feed is "infinitely scrolling", with
     * newly loaded entries being concatenated to the [entries] list.
     */

    data class CatalogFeedWithoutGroups(
      override val arguments: CatalogFeedArguments,
      val entries: LiveData<PagedList<FeedEntry>>,
      val facetsInOrder: List<FeedFacet>,
      val facetsByGroup: Map<String, List<FeedFacet>>,
      override val search: FeedSearch?,
      override val title: String
    ) : CatalogFeedLoaded()

    /**
     * A feed was loaded and it turned out to be a navigation feed.
     */

    data class CatalogFeedNavigation(
      override val arguments: CatalogFeedArguments,
      override val search: FeedSearch?,
      override val title: String
    ) : CatalogFeedLoaded()

    /**
     * A feed was loaded, but it turned out to be empty.
     */

    data class CatalogFeedEmpty(
      override val arguments: CatalogFeedArguments,
      override val search: FeedSearch?,
      override val title: String
    ) : CatalogFeedLoaded()
  }
}
