package org.nypl.simplified.taskrecorder.api

import com.google.common.base.Preconditions

/**
 * A task step recorder.
 *
 * @param <E> The precise type of associated error values
 */

class TaskRecorder<E> private constructor() : TaskRecorderType<E> {

  companion object {

    /**
     * Create a new task recorder.
     */

    fun <E> create(): TaskRecorderType<E> =
      TaskRecorder()
  }

  private val steps = mutableListOf<TaskStep<E>>()

  override fun beginNewStep(message: String): TaskStep<E> {
    val step = TaskStep<E>(description = message)
    this.steps.add(step)
    return step
  }

  override fun currentStepSucceeded(message: String): TaskStep<E> {
    Preconditions.checkState(!this.steps.isEmpty(), "A step must be active")

    val step = this.steps.last()
    step.resolution = TaskStepResolution.TaskStepSucceeded(message)
    return step
  }

  override fun currentStepFailed(
    message: String,
    errorValue: E,
    exception: Throwable?
  ): TaskStep<E> {
    Preconditions.checkState(!this.steps.isEmpty(), "A step must be active")

    val step = this.steps.last()
    step.resolution = TaskStepResolution.TaskStepFailed(
      message = message,
      errorValue = errorValue,
      exception = exception)
    return step
  }

  override fun currentStepFailedAppending(
    message: String,
    errorValue: E,
    exception: Throwable): TaskStep<E> {
    Preconditions.checkState(!this.steps.isEmpty(), "A step must be active")

    val step = this.steps.last()
    return when (val resolution = step.resolution) {
      is TaskStepResolution.TaskStepSucceeded -> {
        step.resolution = TaskStepResolution.TaskStepFailed(message, errorValue, exception)
        step
      }
      is TaskStepResolution.TaskStepFailed -> {
        when (val ex = resolution.exception) {
          null -> {
            step.resolution = resolution.copy(exception = exception)
            step
          }
          else -> {
            if (ex != exception) {
              ex.addSuppressed(exception)
            }
            step
          }
        }
      }
    }
  }


  override fun currentStep(): TaskStep<E>? =
    this.steps.lastOrNull()

  override fun <A> finishSuccess(result: A): TaskResult.Success<E, A> =
    TaskResult.Success(result, this.steps)

  override fun <A> finishFailure(): TaskResult.Failure<E, A> =
    TaskResult.Failure(this.steps)
}