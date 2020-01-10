package org.nypl.simplified.migration.from3master

import android.os.Environment

/**
 * The default Environment implementation.
 */

class EnvironmentQueriesDefault : EnvironmentQueriesType {

  override fun getExternalStorageState(): String =
    Environment.getExternalStorageState()

  override fun isExternalStorageRemovable(): Boolean =
    Environment.isExternalStorageRemovable()
}
