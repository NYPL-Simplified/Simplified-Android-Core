package org.nypl.simplified.migration.from3master

import android.content.res.Resources

class MigrationFrom3MasterStrings(
  private val resources: Resources) : MigrationFrom3MasterStringResourcesType {

  override fun errorBookCopyFailure(title: String): String =
    this.resources.getString(R.string.errorBookCopyFailure, title)

  override fun errorBookAdobeDRMCopyFailure(title: String): String =
    this.resources.getString(R.string.errorBookAdobeDRMCopyFailure, title)

  override fun errorBookmarksCopyFailure(title: String): String =
    this.resources.getString(R.string.errorBookmarksCopyFailure, title)

  override fun successCopiedBookmarks(title: String, count: Int): String =
    this.resources.getString(R.string.reportCopiedBookmarks, title, count)

  override fun errorBookmarksParseFailure(title: String): String =
    this.resources.getString(R.string.errorBookmarksParseFailure, title)

  override fun successCreatedAccount(title: String): String =
    this.resources.getString(R.string.reportCreatedAccount, title)

  override fun successCopiedBook(title: String): String =
    this.resources.getString(R.string.reportCopiedBook, title)

  override fun successAuthenticatedAccount(title: String): String =
    this.resources.getString(R.string.successAuthenticatedAccount, title)

  override fun errorBookLoadTitledFailure(title: String): String =
    this.resources.getString(R.string.errorBookLoadTitledFailure, title)

  override fun errorBookLoadFailure(entry: String): String =
    this.resources.getString(R.string.errorBookLoadFailure, entry.substring(0, 7))

  override fun errorBookUnexpectedFormat(title: String, receivedFormat: String): String =
    this.resources.getString(R.string.errorUnexpectedFormat, title, receivedFormat)

  override fun errorAccountLoadFailure(id: Int): String =
    this.resources.getString(R.string.errorAccountLoadFailure, id)

  override fun errorUnknownAccountProvider(id: Int): String =
    this.resources.getString(R.string.errorUnknownAccountProvider)
}