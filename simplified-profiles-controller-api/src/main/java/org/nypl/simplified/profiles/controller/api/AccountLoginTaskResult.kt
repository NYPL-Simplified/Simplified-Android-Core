package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData
import org.nypl.simplified.taskrecorder.api.TaskStep

/**
 * The result of logging in.
 */

data class AccountLoginTaskResult(
  val steps: List<TaskStep<AccountLoginErrorData>>) {

  /**
   * `true` if the last step in the task failed.
   */

  val failed: Boolean
    get() = this.steps.lastOrNull()?.failed ?: false

}
