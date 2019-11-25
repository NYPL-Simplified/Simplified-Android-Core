package org.nypl.simplified.ui.catalog

import android.os.Parcelable
import io.reactivex.Observable
import org.nypl.simplified.feeds.api.FeedFacet
import java.net.URI

/**
 * The interface exposed by catalog feed view models.
 */

interface CatalogFeedViewModelType {

  /**
   * An observable value that publishes events each time the value of [feedState] has changed.
   */

  val feedStatus: Observable<Unit>

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

  /**
   * Resolve a given facet as a set of feed arguments.
   *
   * @param facet The facet
   */

  fun resolveFacet(
    facet: FeedFacet
  ): CatalogFeedArguments

  /**
   * Store the layout state of the feed-with-groups view. This is typically the
   * result of calling [androidx.recyclerview.widget.LinearLayoutManager.onSaveInstanceState].
   */

  fun saveFeedWithGroupsViewState(state: Parcelable?)

  /**
   * Return the state that was previously stored with [saveFeedWithGroupsViewState].
   */

  fun restoreFeedWithGroupsViewState(): Parcelable?

  /**
   * Store the layout state of the feed-without-groups view. This is typically the
   * result of calling [androidx.recyclerview.widget.LinearLayoutManager.onSaveInstanceState].
   */

  fun saveFeedWithoutGroupsViewState(state: Parcelable?)

  /**
   * Return the state that was previously stored with [saveFeedWithoutGroupsViewState].
   */

  fun restoreFeedWithoutGroupsViewState(): Parcelable?
}