package org.nypl.simplified.taskrecorder.api

import com.google.common.base.Preconditions
import org.nypl.simplified.presentableerror.api.Presentables
import org.slf4j.LoggerFactory

/**
 * A task step recorder.
 */

class TaskRecorder private constructor() : TaskRecorderType {

  private val logger =
    LoggerFactory.getLogger(TaskRecorder::class.java)

  companion object {

    /**
     * Create a new task recorder.
     */

    fun create(): TaskRecorderType =
      TaskRecorder()
  }

  private val steps = mutableListOf<TaskStep>()
  private val attributes = mutableMapOf<String, String>()

  override fun addAttribute(
    name: String,
    value: String
  ) {
    Presentables.putRetry(this.attributes, name, value)
  }

  override fun addAttributes(attributes: Map<String, String>) {
    for ((key, value) in attributes) {
      this.addAttribute(key, value)
    }
  }

  override fun beginNewStep(message: String): TaskStep {
    this.logger.debug("step started: {}", message)

    val step = TaskStep(description = message)
    this.steps.add(step)
    return step
  }

  override fun currentStepSucceeded(message: String): TaskStep {
    Preconditions.checkState(this.steps.isNotEmpty(), "A step must be active")

    this.logger.debug("step succeeded: {}", message)
    val step = this.steps.last()
    step.resolution = TaskStepResolution.TaskStepSucceeded(message)
    return step
  }

  override fun currentStepFailed(
    message: String,
    errorCode: String,
    exception: Throwable?
  ): TaskStep {
    Preconditions.checkState(this.steps.isNotEmpty(), "A step must be active")

    this.logger.debug("step failed: {} ({}): ", message, errorCode, exception)
    val step = this.steps.last()
    step.resolution =
      TaskStepResolution.TaskStepFailed(
        message = message,
        errorCode = errorCode,
        exception = exception
      )
    return step
  }

  override fun currentStepFailedAppending(
    message: String,
    errorCode: String,
    exception: Throwable
  ): TaskStep {
    Preconditions.checkState(this.steps.isNotEmpty(), "A step must be active")

    this.logger.debug("step failed: {} ({}): ", message, errorCode, exception)
    val step = this.steps.last()
    return when (val resolution = step.resolution) {
      is TaskStepResolution.TaskStepSucceeded -> {
        step.resolution = TaskStepResolution.TaskStepFailed(
          message = message,
          exception = exception,
          errorCode = errorCode
        )
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

  override fun addAll(steps: List<TaskStep>) {
    this.steps.addAll(steps)
  }

  override fun currentStep(): TaskStep? =
    this.steps.lastOrNull()

  override fun <A> finishSuccess(result: A): TaskResult.Success<A> =
    TaskResult.Success(result, this.steps, this.attributes.toMap())

  override fun <A> finishFailure(): TaskResult.Failure<A> =
    TaskResult.Failure(this.steps, this.attributes.toMap())
}
