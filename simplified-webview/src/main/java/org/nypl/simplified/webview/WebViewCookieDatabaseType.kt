package org.nypl.simplified.webview

import java.io.Closeable

interface WebViewCookieDatabaseType : Closeable {
  fun getAll(): List<WebViewCookieType>
}
