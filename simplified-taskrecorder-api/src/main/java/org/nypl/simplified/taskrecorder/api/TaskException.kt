package org.nypl.simplified.taskrecorder.api

/**
 * The base type of book controller exceptions.
 */

open class TaskException : Exception {

  val steps: List<TaskStep<*>>

  /**
   * Construct an exception.
   */

  constructor(
    message: String,
    steps: List<TaskStep<*>>
  ) : super(message) {
    this.steps = steps
  }

  /**
   * Construct an exception.
   */

  constructor(
    message: String,
    cause: Throwable,
    steps: List<TaskStep<*>>
  ) : super(message, cause) {
    this.steps = steps
  }

  /**
   * Construct an exception.
   */

  constructor(
    cause: Throwable,
    steps: List<TaskStep<*>>
  ) : super(cause) {
    this.steps = steps
  }
}
