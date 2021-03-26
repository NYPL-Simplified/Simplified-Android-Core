package org.nypl.simplified.feeds.api

import com.google.common.util.concurrent.FluentFuture
import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.nypl.simplified.accounts.api.AccountID
import java.net.URI

/**
 * The type of feed loaders.
 */

interface FeedLoaderType {

  /**
   * `true` if feeds should contain only books that the application can open
   */

  var showOnlySupportedBooks: Boolean

  /**
   * Load a feed from the given URI.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   *
   * @return A future that can be used to cancel the loading feed
   */

  fun fetchURI(
    account: AccountID,
    uri: URI,
    auth: LSHTTPAuthorizationType?,
    method: String
  ): FluentFuture<FeedLoaderResult>

  /**
   * Load a feed from the given URI. For each returned entry in the feed, the local book database
   * is examined and any matching entries are replaced with the data most recently written into the
   * database.
   *
   * @param uri      The URI
   * @param auth     HTTP authentication details, if any
   *
   * @return A future that can be used to cancel the loading feed
   */

  fun fetchURIWithBookRegistryEntries(
    account: AccountID,
    uri: URI,
    auth: LSHTTPAuthorizationType?,
    method: String
  ): FluentFuture<FeedLoaderResult>
}
