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

  override fun currentStepSucceeded(message: String) {
    Preconditions.checkState(!this.steps.isEmpty(), "A step must be active")

    val step = this.steps.last()
    step.resolution = message
    step.failed = false
    step.exception = null
  }

  override fun currentStepFailed(
    message: String,
    errorValue: E?,
    exception: Exception?
  ) {
    Preconditions.checkState(!this.steps.isEmpty(), "A step must be active")

    val step = this.steps.last()
    step.resolution = message
    step.failed = true
    step.errorValue = errorValue
    step.exception = exception
  }

  override fun currentStep(): TaskStep<E>? =
    this.steps.lastOrNull()

  override fun finish(): List<TaskStep<E>> =
    this.steps.toList()
}