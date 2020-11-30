package org.nypl.simplified.accounts.api

/**
 * An HTTP cookie belonging to an account.
 */

data class AccountCookie(
  /**
   * The URL for which the cookie is to be set.
   */

  val url: String,

  /**
   * The cookie as a string, using the format of the Set-Cookie HTTP response header. This includes
   * the name/value pair, as well as metadata attributes.
   */

  val value: String
)
