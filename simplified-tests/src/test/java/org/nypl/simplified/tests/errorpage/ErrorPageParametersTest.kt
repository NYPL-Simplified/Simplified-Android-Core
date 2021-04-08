package org.nypl.simplified.tests.errorpage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

class ErrorPageParametersTest {

  @Test
  fun testReportMultipleTaskSteps() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(),
      taskSteps = listOf(
        TaskStep(
          description = "Doing something...",
          resolution = TaskStepResolution.TaskStepSucceeded(
            message = "Done!"
          )
        ),
        TaskStep(
          description = "Doing another thing...",
          resolution = TaskStepResolution.TaskStepSucceeded(
            message = "Cool."
          )
        ),
        TaskStep(
          description = "Finishing up...",
          resolution = TaskStepResolution.TaskStepFailed(
            message = "Oh no, it failed.",
            errorCode = "FAIL",
            exception = Exception()
          )
        )
      )
    )

    assertEquals(
      """
      Steps:
      1. Doing something...
      ✔️ Done!

      2. Doing another thing...
      ✔️ Cool.

      3. Finishing up...
      ❌ Oh no, it failed.
      """.trimIndent(),
      parameters.reportTaskSteps
    )
  }

  @Test
  fun testReportSingleTaskStep() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(),
      taskSteps = listOf(
        TaskStep(
          description = "Doing something...",
          resolution = TaskStepResolution.TaskStepFailed(
            message = "Nooooooo!",
            errorCode = "FAIL",
            exception = Exception()
          )
        )
      )
    )

    assertEquals(
      """
      Steps:
      1. Doing something...
      ❌ Nooooooo!
      """.trimIndent(),
      parameters.reportTaskSteps
    )
  }

  @Test
  fun testReportEmptyTaskSteps() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(),
      taskSteps = listOf()
    )

    assertEquals("", parameters.reportTaskSteps)
  }

  @Test
  fun testReportMultipleAttributes() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(
        Pair("Account", "12345"),
        Pair("Book", "Frankenstein"),
        Pair("Author", "Mary Shelley")
      ),
      taskSteps = listOf()
    )

    assertEquals(
      """
      Account:
      12345

      Author:
      Mary Shelley

      Book:
      Frankenstein
      """.trimIndent(),
      parameters.reportAttributes
    )
  }

  @Test
  fun testReportSingleAttribute() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(
        Pair("Account", "12345")
      ),
      taskSteps = listOf()
    )

    assertEquals(
      """
      Account:
      12345
      """.trimIndent(),
      parameters.reportAttributes
    )
  }

  @Test
  fun testReportEmptyAttributes() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(),
      taskSteps = listOf()
    )

    assertEquals("", parameters.reportAttributes)
  }

  @Test
  fun testReportBodyOnly() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "There was an error!",
      attributes = sortedMapOf(),
      taskSteps = listOf()
    )

    assertEquals("There was an error!", parameters.report)
  }

  @Test
  fun testReportNoBody() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "",
      attributes = sortedMapOf(
        Pair("Account", "12345")
      ),
      taskSteps = listOf(
        TaskStep(
          description = "Doing something...",
          resolution = TaskStepResolution.TaskStepFailed(
            message = "Nooooooo!",
            errorCode = "FAIL",
            exception = Exception()
          )
        )
      )
    )

    assertEquals(
      """
      Account:
      12345

      Steps:
      1. Doing something...
      ❌ Nooooooo!
      """.trimIndent(),
      parameters.report
    )
  }

  @Test
  fun testReportBodyAttributesTaskSteps() {
    val parameters = ErrorPageParameters(
      emailAddress = "",
      subject = "",
      body = "There was an error!",
      attributes = sortedMapOf(
        Pair("Account", "12345")
      ),
      taskSteps = listOf(
        TaskStep(
          description = "Doing something...",
          resolution = TaskStepResolution.TaskStepFailed(
            message = "Nooooooo!",
            errorCode = "FAIL",
            exception = Exception()
          )
        )
      )
    )

    assertEquals(
      """
      There was an error!

      Account:
      12345

      Steps:
      1. Doing something...
      ❌ Nooooooo!
      """.trimIndent(),
      parameters.report
    )
  }
}
