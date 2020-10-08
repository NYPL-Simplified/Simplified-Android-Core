package org.nypl.simplified.taskrecorder.api

import java.io.Serializable

/**
 * The resolution of a task step.
 */

sealed class TaskStepResolution : Serializable {

  /**
   * The step resolution message.
   */

  abstract val message: String

  /**
   * The exception associated with the step resolution.
   */

  abstract val exception: Throwable?

  /**
   * The task succeeded.
   */

  data class TaskStepSucceeded(
    override val message: String
  ) : TaskStepResolution() {
    override val exception: Throwable? = null
  }

  /**
   * The task failed.
   */

  data class TaskStepFailed(
    override val message: String,
    override val exception: Throwable?,
    val errorCode: String
  ) : TaskStepResolution()
}
