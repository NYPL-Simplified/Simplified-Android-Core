package org.nypl.simplified.webview

import android.database.sqlite.SQLiteDatabase

class WebViewCookieDatabaseV10 internal constructor(
  private val db: SQLiteDatabase
) : WebViewCookieDatabase(db) {
  override fun getAll(): List<WebViewCookieType> {
    val result = mutableListOf<Cookie>()

    val columns = arrayOf(
      "host_key",
      "name",
      "value",
      "path",
      "expires_utc",
      "is_secure",
      "is_httponly",
      "firstpartyonly"
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
            expiresUTC = cursor.getLong(4),
            isSecure = cursor.getInt(5),
            isHttpOnly = cursor.getInt(6),
            firstPartyOnly = cursor.getInt(7)
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
    val expiresUTC: Long,
    val isSecure: Int,
    val isHttpOnly: Int,
    val firstPartyOnly: Int
  ) : WebViewCookieType {

    override val sourceURL: String
      get() {
        val domain = this.hostKey.trimStart('.')

        return (if (this.isSecure > 0) "https" else "http") + "://$domain"
      }

    override fun toSetCookieString(): String {
      val pairs = mutableListOf<List<String>>()

      pairs.add(listOf(this.name, this.value))
      pairs.add(listOf("Domain", this.hostKey))
      pairs.add(listOf("Path", this.path))

      if (this.expiresUTC > 0) {
        pairs.add(listOf("Expires", WebViewUtilities.formatWebKitTimestampForHTTP(this.expiresUTC)))
      }

      if (this.isSecure > 0) {
        pairs.add(listOf("Secure"))
      }

      if (this.isHttpOnly > 0) {
        pairs.add(listOf("HttpOnly"))
      }

      when (this.firstPartyOnly) {
        1 -> pairs.add(listOf("SameSite", "Lax"))
        2 -> pairs.add(listOf("SameSite", "Strict"))
      }

      return pairs.map({ pair ->
        pair.joinToString("=")
      }).joinToString("; ")
    }
  }
}
