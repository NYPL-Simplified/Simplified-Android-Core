package org.nypl.simplified.ui.catalog

import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedLoaderResult
import java.util.UUID

/**
 * The state of a given feed.
 */

sealed class CatalogFeedState {

  /**
   * The unique identifier of the request.
   */

  abstract val requestId: UUID

  /**
   * The arguments used to produce the feed
   */

  abstract val arguments: CatalogFeedArguments

  data class CatalogFeedLoading(
    override val requestId: UUID,
    override val arguments: CatalogFeedArguments,
    val future: FluentFuture<FeedLoaderResult>
  ) : CatalogFeedState()

  data class CatalogFeedLoadFailed(
    override val requestId: UUID,
    override val arguments: CatalogFeedArguments,
    val failure: FeedLoaderResult.FeedLoaderFailure
  ) : CatalogFeedState()

  sealed class CatalogFeedLoaded : CatalogFeedState() {

    data class CatalogFeedWithGroups(
      override val requestId: UUID,
      override val arguments: CatalogFeedArguments,
      val feed: Feed.FeedWithGroups)
      : CatalogFeedLoaded()

    data class CatalogFeedWithoutGroups(
      override val requestId: UUID,
      override val arguments: CatalogFeedArguments,
      val feed: Feed.FeedWithoutGroups)
      : CatalogFeedLoaded()

    data class CatalogFeedNavigation(
      override val requestId: UUID,
      override val arguments: CatalogFeedArguments)
      : CatalogFeedLoaded()
  }
}
