package org.nypl.simplified.buildconfig.api

import java.util.regex.Pattern

/**
 * The URI scheme used for OAuth callbacks.
 */

data class BuildConfigOAuthScheme(val scheme: String) {
  init {
    if (!VALID_SCHEME_PATTERN.matcher(
        this.scheme
      ).matches()
    ) {
      throw IllegalArgumentException("Scheme '${this.scheme}' must match '$VALID_SCHEME_PATTERN'")
    }
  }

  companion object {
    private val VALID_SCHEME_PATTERN = Pattern.compile("[a-z._\\-]+")
  }
}
