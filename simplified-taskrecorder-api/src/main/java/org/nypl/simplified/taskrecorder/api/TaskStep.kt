package org.nypl.simplified.taskrecorder.api

import org.nypl.simplified.presentableerror.api.PresentableType
import java.io.Serializable

/**
 * A step in a task.
 *
 * @param <E> The precise type of associated error values
 */

data class TaskStep(

  /**
   * A humanly-readable, localized description of the task step.
   */

  val description: String,

  /**
   * A humanly-readable, localized description of the resolution task step.
   */

  var resolution: TaskStepResolution = TaskStepResolution.TaskStepSucceeded("")
) : Serializable, PresentableType {
  override val message: String
    get() = this.resolution.message
}
