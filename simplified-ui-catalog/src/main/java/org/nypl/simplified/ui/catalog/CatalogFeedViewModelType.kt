package org.nypl.simplified.ui.catalog

import android.os.Parcelable
import io.reactivex.Observable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedSearch
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
   * Resolve a given URI as a remote feed. The URI, if non-absolute, is resolved against
   * the current feed arguments in order to produce new arguments to load another feed. This
   * method is intended to be called from book detail contexts, where there may not be a
   * feed accessible that has unambiguous account ownership information (ownership can be
   * per-book, and feeds can contain a mix of accounts).
   *
   * @param accountID The account ID that owns the book
   * @param title The title of the target feed
   * @param uri The URI of the target feed
   */

  fun resolveFeedFromBook(
    accountID: AccountID,
    title: String,
    uri: URI
  ): CatalogFeedArguments

  /**
   * Reload the current feed using the given arguments.
   */

  fun reloadFeed(arguments: CatalogFeedArguments)

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

  /**
   * Execute the given search based on the current feed.
   */

  fun resolveSearch(
    search: FeedSearch,
    query: String
  ): CatalogFeedArguments

  /**
   * Set synthesized birthdate based on if user is over 13
   * */
  fun updateBirthYear(over13: Boolean)
}
