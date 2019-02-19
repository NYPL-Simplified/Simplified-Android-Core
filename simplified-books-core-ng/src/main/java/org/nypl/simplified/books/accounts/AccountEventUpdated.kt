package org.nypl.simplified.books.accounts

/**
 * An account was updated.
 */

data class AccountEventUpdated(
  val accountID: AccountID)
  : AccountEvent()
