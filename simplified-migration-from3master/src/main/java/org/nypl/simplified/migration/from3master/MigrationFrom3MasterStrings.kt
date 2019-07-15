package org.nypl.simplified.migration.from3master

import android.content.res.Resources

class MigrationFrom3MasterStrings(
  private val resources: Resources) : MigrationFrom3MasterStringResourcesType {

  override fun reportCreatedAccount(title: String): String =
    this.resources.getString(R.string.reportCreatedAccount, title)

  override fun reportCopiedBook(title: String): String =
    this.resources.getString(R.string.reportCopiedBook, title)

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