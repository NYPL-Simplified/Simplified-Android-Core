package org.nypl.simplified.ui.catalog

import org.nypl.simplified.observable.ObservableType
import java.net.URI

/**
 * The interface exposed by catalog feed view models.
 */

interface CatalogFeedViewModelType {

  /**
   * The status of the current feed.
   */

  val feedStatus: ObservableType<Unit>

  /**
   * Retrieve the status of the current feed, or load a new feed using the initial feed
   * arguments defined for this view model.
   *
   * @see [initialFeedArguments]
   */

  fun feedState(): CatalogFeedState

  /**
   * Resolve and load a given URI as a remote feed. The URI, if non-absolute, is resolved against
   * the URI at the top of the current request stack (assuming that the top of the stack refers
   * to a remote feed).
   *
   * @param title The title of the feed
   * @param uri The URI of the remote feed
   * @param isSearchResults `true` if the feed refers to search results
   */

  fun resolveAndLoadFeed(
    title: String,
    uri: URI,
    isSearchResults: Boolean
  ): CatalogFeedState
}