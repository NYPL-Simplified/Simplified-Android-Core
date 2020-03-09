package org.nypl.simplified.accounts.api

import org.nypl.simplified.presentableerror.api.PresentableErrorType

/**
 * An error value that gives details on why an account could not be deleted.
 */

sealed class AccountDeleteErrorDetails : PresentableErrorType {

  /**
   * It's not possible to delete the last account.
   */

  data class AccountCannotDeleteLastAccount(
    override val message: String
  ) : AccountDeleteErrorDetails()

  /**
   * An unexpected exception occurred.
   */

  data class AccountUnexpectedException(
    override val message: String,
    override val exception: Throwable
  ) : AccountDeleteErrorDetails()
}
