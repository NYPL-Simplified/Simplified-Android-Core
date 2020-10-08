package org.nypl.simplified.taskrecorder.api

import com.google.common.base.Preconditions
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.presentableerror.api.PresentableType
import org.nypl.simplified.presentableerror.api.Presentables.mergeAttributes
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepFailed

/**
 * The result of executing a task.
 */

sealed class TaskResult<A> : PresentableType {

  abstract val steps: List<TaskStep>

  /**
   * A task succeeded.
   */

  data class Success<A>(
    val result: A,
    override val steps: List<TaskStep>,
    override val attributes: Map<String, String>
  ) : TaskResult<A>() {
    init {
      Preconditions.checkArgument(
        this.steps.isNotEmpty(),
        "Must have logged at least one step"
      )
    }

    override val message: String
      get() = this.steps.last().message
  }

  /**
   * A task failed.
   */

  data class Failure<A>(
    override val steps: List<TaskStep>,
    override val attributes: Map<String, String>
  ) : TaskResult<A>(), PresentableErrorType {
    init {
      Preconditions.checkArgument(
        this.steps.isNotEmpty(),
        "Must have logged at least one step"
      )
    }

    override val exception: Throwable?
      get() = this.steps.last().resolution.exception
    override val message: String
      get() = this.steps.last().message

    val lastErrorCode: String
      get() = run {
        val lastStep = this.steps.last { step -> step.resolution is TaskStepFailed }
        return (lastStep.resolution as TaskStepFailed).errorCode
      }
  }

  /**
   * @return The resolution of step `step`
   */

  fun resolutionOf(step: Int): TaskStepResolution =
    this.steps[step].resolution

  /**
   * Functor map for task results.
   */

  fun <B> map(f: (A) -> B): TaskResult<B> {
    return when (this) {
      is Success ->
        Success(
          result = f(this.result),
          steps = this.steps,
          attributes = this.attributes
        )
      is Failure ->
        Failure(
          steps = this.steps,
          attributes = this.attributes
        )
    }
  }

  /**
   * Monadic bind for task results.
   */

  fun <B> flatMap(f: (A) -> TaskResult<B>): TaskResult<B> {
    return when (this) {
      is Success -> {
        when (val next = f.invoke(this.result)) {
          is Success ->
            Success(
              result = next.result,
              steps = this.steps.plus(next.steps),
              attributes = mergeAttributes(this.attributes, next.attributes)
            )
          is Failure ->
            Failure(
              steps = this.steps.plus(next.steps),
              attributes = mergeAttributes(this.attributes, next.attributes)
            )
        }
      }
      is Failure ->
        Failure(
          steps = this.steps,
          attributes = this.attributes
        )
    }
  }

  companion object {

    /**
     * Create a task result that indicates that a task immediately failed with the
     * given error.
     */

    fun <A> fail(
      description: String,
      resolution: String,
      errorCode: String
    ): TaskResult<A> {
      return Failure(
        attributes = mapOf(),
        steps = listOf(
          TaskStep(
            description = description,
            resolution = TaskStepFailed(
              message = resolution,
              errorCode = errorCode,
              exception = null
            )
          )
        )
      )
    }
  }
}
