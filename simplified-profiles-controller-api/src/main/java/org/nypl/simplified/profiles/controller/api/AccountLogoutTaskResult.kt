package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.taskrecorder.api.TaskStep

/**
 * The result of logging out.
 */

data class AccountLogoutTaskResult(
  val steps: List<TaskStep<AccountLogoutErrorData>>) {

  /**
   * `true` if the last step in the task failed.
   */

  val failed: Boolean
    get() = this.steps.lastOrNull()?.failed ?: false

}
