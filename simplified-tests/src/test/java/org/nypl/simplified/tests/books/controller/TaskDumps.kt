package org.nypl.simplified.tests.books.controller

import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.slf4j.Logger

object TaskDumps {

  fun dump(logger: Logger, results: TaskResult<*>) {
    logger.debug("RESULTS:")

    for (step in results.steps) {
      logger.debug("step description: {}", step.description)
      when (val resolution = step.resolution) {
        is TaskStepResolution.TaskStepSucceeded -> {
          logger.debug("step resolution:  {}", resolution.message)
          logger.debug("--")
        }
        is TaskStepResolution.TaskStepFailed -> {
          logger.debug(
            "step resolution:  {} (exception: {}) (error: {})",
            resolution.message, resolution.exception, resolution.errorCode
          )
          logger.debug("--")
        }
      }
    }
  }
}
