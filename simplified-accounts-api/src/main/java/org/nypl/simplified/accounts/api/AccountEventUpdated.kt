package org.nypl.simplified.accounts.api

/**
 * An account was updated.
 */

data class AccountEventUpdated(
  override val message: String,
  val accountID: AccountID
) : AccountEvent()
