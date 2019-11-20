package org.nypl.simplified.ui.catalog

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderResult

/**
 * The state of a given feed.
 */

sealed class CatalogFeedState {

  /**
   * The arguments used to produce the feed
   */

  abstract val arguments: CatalogFeedArguments

  data class CatalogFeedLoading(
    override val arguments: CatalogFeedArguments,
    val future: FluentFuture<FeedLoaderResult>
  ) : CatalogFeedState()

  data class CatalogFeedLoadFailed(
    override val arguments: CatalogFeedArguments,
    val failure: FeedLoaderResult.FeedLoaderFailure
  ) : CatalogFeedState()

  sealed class CatalogFeedLoaded : CatalogFeedState() {

    data class CatalogFeedWithGroups(
      override val arguments: CatalogFeedArguments,
      val feed: Feed.FeedWithGroups)
      : CatalogFeedLoaded()

    data class CatalogFeedWithoutGroups(
      override val arguments: CatalogFeedArguments,
      val pagedList: LiveData<PagedList<FeedEntry>>)
      : CatalogFeedLoaded()

    data class CatalogFeedNavigation(
      override val arguments: CatalogFeedArguments)
      : CatalogFeedLoaded()
  }
}
