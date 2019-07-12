package org.nypl.simplified.migration.from3master

/**
 * A class to make it possible to mock [android.os.Environment].
 */

interface EnvironmentQueriesType {

  /**
   * @see android.os.Environment.getExternalStorageState
   */

  fun getExternalStorageState(): String

  /**
   * @see android.os.Environment.isExternalStorageRemovable
   */

  fun isExternalStorageRemovable(): Boolean

}