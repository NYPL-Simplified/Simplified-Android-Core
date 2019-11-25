package org.nypl.simplified.ui.catalog

import androidx.paging.PageKeyedDataSource
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Option
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * A data source used for infinitely-scrolling feeds without groups. The data source
 * is seeded with an initial feed, and the "next" links in the feed are used to load
 * subsequent data.
 */

class CatalogPagedDataSource(
  private val feedLoader: FeedLoaderType,
  private val initialFeed: Feed.FeedWithoutGroups,
  private val profilesController: ProfilesControllerType
) : PageKeyedDataSource<URI, FeedEntry>() {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedDataSource::class.java)

  private val authenticationCredentials =
    Option.of(this.profilesController.profileAccountCurrent()
      .loginState
      .credentials)
      .map { credentials ->
        AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials!!)
      }

  override fun loadInitial(
    params: LoadInitialParams<URI>,
    callback: LoadInitialCallback<URI, FeedEntry>
  ) {
    Preconditions.checkArgument(
      this.initialFeed.entriesInOrder.isNotEmpty(),
      "Do not pass an empty initial feed to the paged data source!")

    callback.onResult(
      this.initialFeed.entriesInOrder,
      0,
      Integer.MAX_VALUE,
      null,
      this.initialFeed.feedNext
    )
  }

  override fun loadAfter(
    params: LoadParams<URI>,
    callback: LoadCallback<URI, FeedEntry>
  ) {
    this.logger.debug("loadAfter: {}", params.key)

    this.feedLoader.fetchURI(params.key, this.authenticationCredentials)
      .map { result ->
        return@map when (result) {
          is FeedLoaderResult.FeedLoaderSuccess -> {
            when (val feed = result.feed) {
              is Feed.FeedWithoutGroups -> {
                this.logger.debug("loadAfter: {}: received feed without groups", params.key)
                callback.onResult(
                  feed.entriesInOrder,
                  feed.feedNext
                )
              }
              is Feed.FeedWithGroups -> {
                this.logger.error("loadAfter: {}: received feed with groups", params.key)
                callback.onResult(
                  mutableListOf(),
                  feed.feedNext
                )
              }
            }
          }
          is FeedLoaderResult.FeedLoaderFailure -> {
            this.logger.error("loadAfter: {}: ", params.key, result.exception)
            callback.onResult(
              mutableListOf(),
              null
            )
          }
        }
      }
  }

  override fun loadBefore(
    params: LoadParams<URI>,
    callback: LoadCallback<URI, FeedEntry>
  ) {
    this.logger.debug("loadBefore: {}", params.key)

    this.feedLoader.fetchURI(params.key, this.authenticationCredentials)
      .map { result ->
        return@map when (result) {
          is FeedLoaderResult.FeedLoaderSuccess -> {
            when (val feed = result.feed) {
              is Feed.FeedWithoutGroups -> {
                this.logger.debug("loadBefore: {}: received feed without groups", params.key)
                callback.onResult(
                  feed.entriesInOrder,
                  feed.feedNext
                )
              }
              is Feed.FeedWithGroups -> {
                this.logger.error("loadBefore: {}: received feed with groups", params.key)
                callback.onResult(
                  mutableListOf(),
                  feed.feedNext
                )
              }
            }
          }
          is FeedLoaderResult.FeedLoaderFailure -> {
            this.logger.error("loadBefore: {}: ", params.key, result.exception)
            callback.onResult(
              mutableListOf(),
              null
            )
          }
        }
      }
  }
}