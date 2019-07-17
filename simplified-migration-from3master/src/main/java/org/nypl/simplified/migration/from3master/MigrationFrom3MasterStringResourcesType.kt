package org.nypl.simplified.migration.from3master

import org.nypl.simplified.migration.spi.MigrationEvent

/**
 * The strings published during migration.
 */

interface MigrationFrom3MasterStringResourcesType {

  val successDeletedOldData: String

  fun errorUnknownAccountProvider(
    id: Int
  ): String

  fun errorAccountLoadFailure(
    id: Int
  ): String

  fun errorBookLoadFailure(
    entry: String
  ): String

  fun errorBookUnexpectedFormat(
    title: String,
    receivedFormat: String
  ): String

  fun errorBookLoadTitledFailure(
    title: String
  ): String

  fun errorBookCopyFailure(
    title: String
  ): String

  fun errorBookAdobeDRMCopyFailure(
    title: String
  ): String

  fun errorBookmarksCopyFailure(
    title: String
  ): String

  fun errorBookmarksParseFailure(
    title: String
  ): String

  fun errorAccountAuthenticationFailure(
    title: String
  ): String

  fun successCreatedAccount(
    title: String
  ): String

  fun successCopiedBook(
    title: String
  ): String

  fun successCopiedBookmarks(
    title: String,
    count: Int
  ): String

  fun successAuthenticatedAccount(
    title: String
  ): String
}
