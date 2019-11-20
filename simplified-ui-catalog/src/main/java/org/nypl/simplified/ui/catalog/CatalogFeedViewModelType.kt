package org.nypl.simplified.ui.catalog

import org.nypl.simplified.observable.ObservableType
import java.net.URI

/**
 * The interface exposed by catalog feed view models.
 */

interface CatalogFeedViewModelType {

  /**
   * An observable value that publishes events each time the value of [feedState] has changed.
   */

  val feedStatus: ObservableType<Unit>

  /**
   * Retrieve the status of the current feed.
   */

  fun feedState(): CatalogFeedState

  /**
   * Resolve a given URI as a remote feed. The URI, if non-absolute, is resolved against
   * the current feed arguments in order to produce new arguments to load another feed.
   *
   * @param title The title of the target feed
   * @param uri The URI of the target feed
   * @param isSearchResults `true` if the target feed refers to search results
   */

  fun resolveFeed(
    title: String,
    uri: URI,
    isSearchResults: Boolean
  ): CatalogFeedArguments
}