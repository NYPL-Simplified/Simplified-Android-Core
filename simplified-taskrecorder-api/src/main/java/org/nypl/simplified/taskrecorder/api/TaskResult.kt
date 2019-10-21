package org.nypl.simplified.taskrecorder.api

import com.google.common.base.Preconditions
import java.io.Serializable

/**
 * The result of executing a task.
 */

sealed class TaskResult<E : Serializable, A> {

  abstract val steps: List<TaskStep<E>>

  /**
   * A task succeeded.
   */

  data class Success<E : Serializable, A>(
    val result: A,
    override val steps: List<TaskStep<E>>)
    : TaskResult<E, A>() {
    init {
      Preconditions.checkArgument(
        this.steps.isNotEmpty(),
        "Must have logged at least one step")
    }
  }

  /**
   * A task failed.
   */

  data class Failure<E : Serializable, A>(
    override val steps: List<TaskStep<E>>)
    : TaskResult<E, A>() {
    init {
      Preconditions.checkArgument(
        this.steps.isNotEmpty(),
        "Must have logged at least one step")
    }

    /**
     * The errors associated with the failure.
     */

    fun errors(): List<E> {
      val errorList = mutableListOf<E>()
      for (step in this.steps) {
        when (val resolution = step.resolution) {
          is TaskStepResolution.TaskStepSucceeded -> Unit
          is TaskStepResolution.TaskStepFailed -> errorList.add(resolution.errorValue)
        }
      }
      return errorList.toList()
    }
  }

  /**
   * Functor map for task results.
   */

  fun <B> map(f: (A) -> B): TaskResult<E, B> {
    return when (this) {
      is Success ->
        Success(
          result = f(this.result),
          steps = this.steps)
      is Failure ->
        Failure(this.steps)
    }
  }

  /**
   * Monadic bind for task results.
   */

  fun <B> flatMap(f: (A) -> TaskResult<E, B>): TaskResult<E, B> {
    return when (this) {
      is Success -> {
        when (val next = f.invoke(this.result)) {
          is Success ->
            Success(
              result = next.result,
              steps = this.steps.plus(next.steps))
          is Failure ->
            Failure(steps = this.steps.plus(next.steps))
        }
      }
      is Failure ->
        Failure(this.steps)
    }
  }
}