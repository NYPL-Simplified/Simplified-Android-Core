package org.nypl.simplified.taskrecorder.api

/**
 * A task recorder. Record the steps of complex tasks to explain how and why errors occurred.
 *
 * @param <E> The precise type of associated error values
 */

interface TaskRecorderType<E> {

  /**
   * Start a new controller task step.
   */

  fun beginNewStep(message: String): TaskStep<E>

  /**
   * Resolve the current step and mark it as having succeeded.
   */

  fun currentStepSucceeded(message: String)

  /**
   * Resolve the current step and mark it as having failed.
   */

  fun currentStepFailed(
    message: String,
    errorValue: E? = null,
    exception: Exception? = null
  )

  /**
   * Complete recording of all steps.
   */

  fun finish(): List<TaskStep<E>>

  /**
   * @return The current step
   */

  fun currentStep(): TaskStep<E>?

}
