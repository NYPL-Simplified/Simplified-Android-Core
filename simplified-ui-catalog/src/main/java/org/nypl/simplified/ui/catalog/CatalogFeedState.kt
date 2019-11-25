package org.nypl.simplified.ui.catalog

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedLoaderResult

/**
 * The state of a given feed.
 */

sealed class CatalogFeedState {

  /**
   * The arguments used to produce the feed
   */

  abstract val arguments: CatalogFeedArguments

  /**
   * The feed is currently loading.
   */

  data class CatalogFeedLoading(
    override val arguments: CatalogFeedArguments,
    val future: FluentFuture<FeedLoaderResult>
  ) : CatalogFeedState()

  /**
   * Loading a feed failed.
   */

  data class CatalogFeedLoadFailed(
    override val arguments: CatalogFeedArguments,
    val failure: FeedLoaderResult.FeedLoaderFailure
  ) : CatalogFeedState()

  sealed class CatalogFeedLoaded : CatalogFeedState() {

    /**
     * A feed was loaded and it turned out to be a feed with groups. These will be
     * rendered as horizontal scrolling lanes.
     */

    data class CatalogFeedWithGroups(
      override val arguments: CatalogFeedArguments,
      val feed: Feed.FeedWithGroups)
      : CatalogFeedLoaded()

    /**
     * A feed was loaded without groups. The feed is "infinitely scrolling", with
     * newly loaded entries being concatenated to the [entries] list.
     */

    data class CatalogFeedWithoutGroups(
      override val arguments: CatalogFeedArguments,
      val entries: LiveData<PagedList<FeedEntry>>,
      val facetsInOrder: List<FeedFacet>,
      val facetsByGroup: Map<String, List<FeedFacet>>)
      : CatalogFeedLoaded()

    /**
     * A feed was loaded and it turned out to be a navigation feed.
     */

    data class CatalogFeedNavigation(
      override val arguments: CatalogFeedArguments)
      : CatalogFeedLoaded()

    /**
     * A feed was loaded, but it turned out to be empty.
     */

    data class CatalogFeedEmpty(
      override val arguments: CatalogFeedArguments
    ) : CatalogFeedLoaded()
  }
}
