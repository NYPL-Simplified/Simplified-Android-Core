package org.nypl.simplified.taskrecorder.api

/**
 * A step in a task.
 *
 * @param <E> The precise type of associated error values
 */

data class TaskStep<E>(

  /**
   * A humanly-readable, localized description of the task step.
   */

  val description: String,

  /**
   * A humanly-readable, localized description of the resolution task step.
   */

  var resolution: TaskStepResolution<E> = TaskStepResolution.TaskStepSucceeded(""))

