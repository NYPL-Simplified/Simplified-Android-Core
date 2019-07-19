package org.nypl.simplified.accounts.api

import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.PresentableType

/**
 * The type of events regarding account creation.
 */

sealed class AccountEventCreation : AccountEvent(), PresentableType {

  /**
   * Creating an account is in progress.
   */

  data class AccountEventCreationInProgress(
    override val message: String)
    : AccountEventCreation()

  /**
   * Creating an account succeeded.
   */

  data class AccountEventCreationSucceeded(
    override val message: String,
    val id: AccountID)
    : AccountEventCreation()

  /**
   * Creating an account failed.
   */

  data class AccountEventCreationFailed(
    override val message: String)
    : AccountEventCreation(), PresentableErrorType

}
