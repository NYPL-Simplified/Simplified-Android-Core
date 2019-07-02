package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.taskrecorder.api.TaskStep

/**
 * The result of creating an account.
 */

data class AccountDeleteTaskResult(
  val steps: List<TaskStep<AccountDeleteErrorDetails>>) {

  /**
   * `true` if the last step in the task failed.
   */

  val failed: Boolean
    get() = this.steps.lastOrNull()?.failed ?: false

}
