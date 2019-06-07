package org.nypl.simplified.profiles.controller.api

import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginErrorData
import org.nypl.simplified.taskrecorder.api.TaskException
import org.nypl.simplified.taskrecorder.api.TaskStep

class AccountLoginTaskException : TaskException {

  /**
   * Construct an exception.
   */

  constructor(
    message: String,
    steps: List<TaskStep<AccountLoginErrorData>>
  ) : super(message, steps)

  /**
   * Construct an exception.
   */

  constructor(
    message: String,
    cause: Throwable,
    steps: List<TaskStep<AccountLoginErrorData>>
  ) : super(message, cause, steps)

  /**
   * Construct an exception.
   */

  constructor(
    cause: Throwable,
    steps: List<TaskStep<AccountLoginErrorData>>
  ) : super(cause, steps)
}
