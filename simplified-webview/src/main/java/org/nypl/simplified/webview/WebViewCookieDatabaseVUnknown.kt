package org.nypl.simplified.webview

import android.database.sqlite.SQLiteDatabase

/**
 * A fallback cookie database implementation, used when there is no implementation for the specific
 * database version. This implementation assumes the presence of a minimal set of columns that are
 * likely to be present.
 */

class WebViewCookieDatabaseVUnknown internal constructor(
  private val db: SQLiteDatabase
) : WebViewCookieDatabase(db) {
  override fun getAll(): List<WebViewCookieType> {
    val result = mutableListOf<Cookie>()

    val columns = arrayOf(
      "host_key",
      "name",
      "value",
      "path",
    )

    this.db.query(
      DB_COOKIE_TABLE_NAME,
      columns,
      null,
      null,
      null,
      null,
      null
    ).use { cursor ->
      while (cursor.moveToNext()) {
        result.add(
          Cookie(
            hostKey = cursor.getString(0),
            name = cursor.getString(1),
            value = cursor.getString(2),
            path = cursor.getString(3),
          )
        )
      }
    }

    return result
  }

  data class Cookie(
    val hostKey: String,
    val name: String,
    val value: String,
    val path: String,
  ) : WebViewCookieType {

    override val sourceURL: String
      get() {
        val domain = this.hostKey.trimStart('.')

        return "https://$domain"
      }

    override fun toSetCookieString(): String {
      val pairs = mutableListOf<List<String>>()

      pairs.add(listOf(this.name, this.value))
      pairs.add(listOf("Domain", this.hostKey))
      pairs.add(listOf("Path", this.path))

      return pairs.map({ pair ->
        pair.joinToString("=")
      }).joinToString("; ")
    }
  }
}
