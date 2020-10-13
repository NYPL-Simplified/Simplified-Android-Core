package org.nypl.simplified.accounts.api

import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.PresentableType
import org.nypl.simplified.taskrecorder.api.TaskResult

/**
 * The type of account deletion events.
 */

sealed class AccountEventDeletion : AccountEvent(), PresentableType {

  /**
   * Deleting an account is in progress.
   */

  data class AccountEventDeletionInProgress(
    override val message: String
  ) : AccountEventDeletion()

  /**
   * Deleting an account succeeded.
   */

  data class AccountEventDeletionSucceeded(
    override val message: String,
    val id: AccountID
  ) : AccountEventDeletion()

  /**
   * Deleting an account failed.
   */

  data class AccountEventDeletionFailed(
    val taskResult: TaskResult.Failure<*>
  ) : AccountEventDeletion(), PresentableErrorType {
    override val message: String
      get() = this.taskResult.message
    override val exception: Throwable?
      get() = this.taskResult.exception
    override val attributes: Map<String, String>
      get() = this.taskResult.attributes
  }
}
