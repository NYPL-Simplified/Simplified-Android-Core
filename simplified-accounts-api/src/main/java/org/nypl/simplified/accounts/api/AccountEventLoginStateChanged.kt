package org.nypl.simplified.accounts.api

import org.nypl.simplified.presentableerror.api.PresentableType

/**
 * The state of an account changed with respect to logging in/out.
 */

data class AccountEventLoginStateChanged(
  override val message: String,
  val accountID: AccountID,
  val state: AccountLoginState
) : AccountEvent(), PresentableType
