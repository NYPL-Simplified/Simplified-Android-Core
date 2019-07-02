package org.nypl.simplified.accounts.api

import com.google.common.base.Preconditions
import org.nypl.simplified.taskrecorder.api.TaskStep

/**
 * The result of resolving an account provider description.
 */

data class AccountProviderResolutionResult(
  val result: AccountProviderType?,
  val steps: List<TaskStep<AccountProviderResolutionErrorDetails>>)  {

  /**
   * `true` if the last step in the task failed.
   */

  val failed: Boolean
    get() = this.steps.lastOrNull()?.failed ?: false

  init {
    Preconditions.checkArgument(
      (this.result == null) == this.failed,
      "A successful resolution must produce a result")
  }
}