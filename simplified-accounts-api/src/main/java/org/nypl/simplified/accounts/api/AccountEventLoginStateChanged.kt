package org.nypl.simplified.accounts.api

/**
 * The state of an account changed with respect to logging in/out.
 */

data class AccountEventLoginStateChanged(
  val accountID: AccountID,
  val state: AccountLoginState)
  : AccountEvent()
