package org.nypl.simplified.accounts.api

/**
 * A token used by Adobe DRM to activate and deactivate devices.
 *
 * This is received by clients in OPDS feeds as a pipe ("|") separated string similar to
 * `NYNYPL|1513878186|5e3cdf28-e3a2-11e7-ab18-0e26ed4612aa|LEcBeSVavfkJRIRd5cRWdUK5p7DZjuoxwwKpoPIqKLA@`.
 */

data class AccountAuthenticationAdobeClientToken(
  val userName: String,
  val password: String,
  val rawToken: String
) {
  companion object {

    /**
     * Parse a raw token.
     */

    fun parse(raw: String): AccountAuthenticationAdobeClientToken {
      val username = raw.substring(0, raw.lastIndexOf("|"))
      val password = raw.substring(raw.lastIndexOf("|") + 1)
      return AccountAuthenticationAdobeClientToken(
        userName = username,
        password = password,
        rawToken = raw
      )
    }
  }
}
