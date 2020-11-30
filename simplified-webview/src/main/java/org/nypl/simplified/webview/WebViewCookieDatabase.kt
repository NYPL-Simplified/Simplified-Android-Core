package org.nypl.simplified.webview

import android.database.sqlite.SQLiteDatabase
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException

abstract class WebViewCookieDatabase(
  private val db: SQLiteDatabase
) : WebViewCookieDatabaseType {

  companion object {
    private val logger = LoggerFactory.getLogger(WebViewCookieDatabase::class.java)

    const val DB_FILE_NAME = "Cookies"
    const val DB_COOKIE_TABLE_NAME = "cookies"
    const val DB_META_TABLE_NAME = "meta"

    /**
     * A factory function for WebViewCookieDatabaseReader instances.
     *
     * @param dataDir The directory that contains web view stored data.
     */

    fun open(
      dataDir: File
    ): WebViewCookieDatabaseType {
      val cookiesFile = this.findCookiesFile(dataDir)

      logger.debug("using cookie database file {}", cookiesFile)

      val db = SQLiteDatabase.openDatabase(
        cookiesFile.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY
      )

      val version = getVersion(db)

      logger.debug("found cookie database version {}", version)

      return when (version) {
        "12" -> WebViewCookieDatabaseV12(db)
        "10" -> WebViewCookieDatabaseV10(db)
        else -> {
          logger.warn("no reader found for cookie database version {} -- cookies may not be read correctly")

          WebViewCookieDatabaseVUnknown(db)
        }
      }
    }

    /**
     * Attempt to locate a cookie database file in the given directory. Different versions of the
     * Android web view may store this file in different locations.
     */

    private fun findCookiesFile(
      dataDir: File
    ): File {
      val candidatePaths = listOf(
        DB_FILE_NAME,
        "Default/$DB_FILE_NAME"
      )

      val path = candidatePaths.find { candidatePath ->
        val candidateFile = File(dataDir, candidatePath)

        logger.debug("checking for cookie database at {}", candidateFile)

        candidateFile.exists()
      }

      if (path == null) {
        throw FileNotFoundException("Could not find a cookie database file")
      }

      return File(dataDir, path)
    }

    /**
     * Returns the version of the cookie database, or null if the version can not be determined.
     */

    private fun getVersion(
      db: SQLiteDatabase
    ): String? {
      try {
        db.query(
          DB_META_TABLE_NAME,
          arrayOf("value"),
          "key = 'last_compatible_version'",
          null,
          null,
          null,
          null
        ).use { cursor ->
          if (cursor.moveToNext()) {
            return cursor.getString(0)
          }
        }
      } catch (e: Exception) {
        logger.error("could not get version from cookie database {}:", db.path, e)
      }

      return null
    }
  }

  override fun close() {
    this.db.close()
  }
}
