package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import java.io.IOException
import java.net.URI

interface ReaderBookmarkHTTPCallsType {

  /**
   * @return `true` if annotation syncing is enabled for the given account
   */

  @Throws(IOException::class)
  fun syncingIsEnabled(
    uri: URI,
    credentials: AccountAuthenticationCredentials): Boolean

  /**
   * Enable or disable annotation syncing for the given account.
   */

  @Throws(IOException::class)
  fun syncingEnable(
    uri: URI,
    credentials: AccountAuthenticationCredentials,
    enabled: Boolean)

  /**
   * Retrieve the list of bookmarks for the given account. This call will fail
   * with an exception if syncing is not enabled.
   *
   * @see #syncingIsEnabled
   * @see #syncingEnable
   */

  @Throws(IOException::class)
  fun bookmarksGet(
    uri: URI,
    credentials: AccountAuthenticationCredentials): List<BookmarkAnnotation>

  /**
   * Add a bookmark for the given account. This call will fail with an exception if
   * syncing is not enabled.
   *
   * @see #syncingIsEnabled
   * @see #syncingEnable
   */

  @Throws(IOException::class)
  fun bookmarkAdd(
    uri: URI,
    credentials: AccountAuthenticationCredentials,
    bookmark: BookmarkAnnotation)
}
