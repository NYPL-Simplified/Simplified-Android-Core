package org.nypl.simplified.taskrecorder.api

import java.io.Serializable

/**
 * The resolution of a task step.
 */

sealed class TaskStepResolution<E : Serializable> : Serializable {

  abstract val message: String

  abstract val exception: Throwable?

  /**
   * The task succeeded.
   */

  data class TaskStepSucceeded<E : Serializable>(
    override val message: String)
    : TaskStepResolution<E>() {
    override val exception: Throwable? = null
  }

  /**
   * The task failed.
   */

  data class TaskStepFailed<E : Serializable>(
    override val message: String,
    val errorValue: E,
    override val exception: Throwable?)
    : TaskStepResolution<E>()

}
