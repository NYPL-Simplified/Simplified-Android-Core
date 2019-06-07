package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutErrorData
import org.nypl.simplified.taskrecorder.api.TaskException
import org.nypl.simplified.taskrecorder.api.TaskStep

class AccountLogoutTaskException : TaskException {

  /**
   * Construct an exception.
   */

  constructor(
    message: String,
    steps: List<TaskStep<AccountLogoutErrorData>>
  ) : super(message, steps)

  /**
   * Construct an exception.
   */

  constructor(
    message: String,
    cause: Throwable,
    steps: List<TaskStep<AccountLogoutErrorData>>
  ) : super(message, cause, steps)

  /**
   * Construct an exception.
   */

  constructor(
    cause: Throwable,
    steps: List<TaskStep<AccountLogoutErrorData>>
  ) : super(cause, steps)
}
