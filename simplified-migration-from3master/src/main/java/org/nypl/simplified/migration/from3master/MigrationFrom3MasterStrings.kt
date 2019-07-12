package org.nypl.simplified.migration.from3master

import android.content.res.Resources

class MigrationFrom3MasterStrings(
  private val resources: Resources) : MigrationFrom3MasterStringResourcesType {

  override fun errorAccountLoadFailure(id: Int): String =
    this.resources.getString(R.string.errorAccountLoadFailure, id)

  override fun errorUnknownAccountProvider(id: Int): String =
    this.resources.getString(R.string.errorUnknownAccountProvider)
}