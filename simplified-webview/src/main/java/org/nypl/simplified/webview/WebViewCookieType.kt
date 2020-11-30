package org.nypl.simplified.webview

/**
 * A cookie stored in the web view cookie database.
 */

interface WebViewCookieType {

  /**
   * The URL for which this cookie was received. This value may be reconstructed from the cookie's
   * attributes.
   */

  val sourceURL: String

  /**
   * Returns a representation of the cookie suitable for use in an HTTP Set-Cookie header, including
   * the name/value and metadata attributes.
   */

  fun toSetCookieString(): String
}
