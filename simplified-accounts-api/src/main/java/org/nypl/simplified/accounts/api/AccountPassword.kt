package org.nypl.simplified.accounts.api

/**
 * An account password.
 */

data class AccountPassword(val value: String) {
  override fun toString(): String {
    return "[REDACTED]"
  }
}
