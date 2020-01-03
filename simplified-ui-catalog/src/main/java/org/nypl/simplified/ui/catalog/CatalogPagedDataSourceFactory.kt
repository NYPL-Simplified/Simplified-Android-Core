package org.nypl.simplified.ui.catalog

import androidx.paging.DataSource
import org.nypl.simplified.feeds.api.Feed.FeedWithoutGroups
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import java.net.URI

/**
 * A factory for producing paged data sources.
 */

class CatalogPagedDataSourceFactory(
  private val feedLoader: FeedLoaderType,
  private val initialFeed: FeedWithoutGroups,
  private val profilesController: ProfilesControllerType
) : DataSource.Factory<URI, FeedEntry>() {

  override fun create(): DataSource<URI, FeedEntry> {
    return CatalogPagedDataSource(
      feedLoader = this.feedLoader,
      initialFeed = this.initialFeed,
      profilesController = this.profilesController
    )
  }
}