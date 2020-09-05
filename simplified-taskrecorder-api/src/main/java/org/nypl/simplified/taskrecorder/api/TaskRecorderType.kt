package org.nypl.simplified.taskrecorder.api

/**
 * A task recorder. Record the steps of complex tasks to explain how and why errors occurred.
 */

interface TaskRecorderType {

  /**
   * Add an attribute to the task.
   */

  fun addAttribute(
    name: String,
    value: String
  )

  /**
   * Add a set of attributes to the task.
   */

  fun addAttributes(attributes: Map<String, String>)

  /**
   * Start a new controller task step.
   */

  fun beginNewStep(message: String): TaskStep

  /**
   * Resolve the current step and mark it as having succeeded.
   */

  fun currentStepSucceeded(message: String): TaskStep

  /**
   * Resolve the current step and mark it as having failed.
   */

  fun currentStepFailed(
    message: String,
    errorCode: String,
    exception: Throwable? = null
  ): TaskStep

  /**
   * If the current step has not failed, fail it with the given error. If the current step
   * has failed but no exception is present, add the given exception. Otherwise, if an exception
   * is present, add a suppressed exception.
   */

  fun currentStepFailedAppending(
    message: String,
    errorCode: String,
    exception: Throwable
  ): TaskStep

  /**
   * Complete recording of all steps.
   */

  fun <A> finishSuccess(result: A): TaskResult.Success<A>

  /**
   * Complete recording of all steps.
   */

  fun <A> finishFailure(): TaskResult.Failure<A>

  /**
   * @return The current step
   */

  fun currentStep(): TaskStep?

  /**
   * Add a series of steps.
   */

  fun addAll(steps: List<TaskStep>)
}
