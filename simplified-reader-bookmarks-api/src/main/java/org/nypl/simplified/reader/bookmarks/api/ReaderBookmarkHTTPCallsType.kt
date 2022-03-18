package org.nypl.simplified.reader.bookmarks.api

import org.nypl.simplified.accounts.api.AccountReadableType
import java.io.IOException
import java.net.URI

interface ReaderBookmarkHTTPCallsType {

  /**
   * @return `true` if annotation syncing is enabled for the given account
   */

  @Throws(IOException::class)
  fun syncingIsEnabled(
    settingsURI: URI,
    account: AccountReadableType
  ): Boolean

  /**
   * Enable or disable annotation syncing for the given account.
   */

  @Throws(IOException::class)
  fun syncingEnable(
    settingsURI: URI,
    account: AccountReadableType,
    enabled: Boolean
  )

  /**
   * Retrieve the list of bookmarks for the given account. This call will fail
   * with an exception if syncing is not enabled.
   *
   * @see #syncingIsEnabled
   * @see #syncingEnable
   */

  @Throws(IOException::class)
  fun bookmarksGet(
    annotationsURI: URI,
    account: AccountReadableType
  ): List<BookmarkAnnotation>

  /**
   * Add a bookmark for the given account. This call will fail with an exception if
   * syncing is not enabled.
   *
   * @see #syncingIsEnabled
   * @see #syncingEnable
   */

  @Throws(IOException::class)
  fun bookmarkAdd(
    annotationsURI: URI,
    bookmark: BookmarkAnnotation,
    account: AccountReadableType
  )

  /**
   * Delete a bookmark for the given account. This call will fail with an exception if
   * syncing is not enabled.
   *
   * @see #syncingIsEnabled
   * @see #syncingEnable
   */

  @Throws(IOException::class)
  fun bookmarkDelete(
    bookmarkURI: URI,
    account: AccountReadableType
  )
}
