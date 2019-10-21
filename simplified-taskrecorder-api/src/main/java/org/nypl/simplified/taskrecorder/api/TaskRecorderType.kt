package org.nypl.simplified.taskrecorder.api

import java.io.Serializable

/**
 * A task recorder. Record the steps of complex tasks to explain how and why errors occurred.
 *
 * @param <E> The precise type of associated error values
 */

interface TaskRecorderType<E : Serializable> {

  /**
   * Start a new controller task step.
   */

  fun beginNewStep(message: String): TaskStep<E>

  /**
   * Resolve the current step and mark it as having succeeded.
   */

  fun currentStepSucceeded(message: String): TaskStep<E>

  /**
   * Resolve the current step and mark it as having failed.
   */

  fun currentStepFailed(
    message: String,
    errorValue: E,
    exception: Throwable? = null
  ): TaskStep<E>

  /**
   * If the current step has not failed, fail it with the given error. If the current step
   * has failed but no exception is present, add the given exception. Otherwise, if an exception
   * is present, add a suppressed exception.
   */

  fun currentStepFailedAppending(
    message: String,
    errorValue: E,
    exception: Throwable
  ): TaskStep<E>

  /**
   * Complete recording of all steps.
   */

  fun <A> finishSuccess(result: A): TaskResult.Success<E, A>

  /**
   * Complete recording of all steps.
   */

  fun <A> finishFailure(): TaskResult.Failure<E, A>

  /**
   * @return The current step
   */

  fun currentStep(): TaskStep<E>?

  /**
   * Add a series of steps.
   */

  fun addAll(steps: List<TaskStep<E>>)

}
