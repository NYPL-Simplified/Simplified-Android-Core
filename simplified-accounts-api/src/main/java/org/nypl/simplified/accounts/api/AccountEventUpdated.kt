package org.nypl.simplified.accounts.api

/**
 * An account was updated.
 */

data class AccountEventUpdated(
  val accountID: AccountID)
  : AccountEvent()
