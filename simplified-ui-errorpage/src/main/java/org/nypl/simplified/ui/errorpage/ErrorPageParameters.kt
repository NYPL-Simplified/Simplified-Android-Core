package org.nypl.simplified.ui.errorpage

import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import java.io.Serializable
import java.util.SortedMap

/**
 * Parameters for instantiating an error page fragment.
 */

data class ErrorPageParameters(

  /**
   * The email address of the technical support provider.
   */

  val emailAddress: String,

  /**
   * The body of the message sent to technical support.
   */

  val body: String,

  /**
   * The subject used for any emails send to technical support.
   */

  val subject: String,

  /**
   * The attributes that will be displayed in the "Error Details" section. If no attributes are
   * provided, the entire section will be hidden.
   */

  val attributes: SortedMap<String, String>,

  /**
   * The steps that lead up to the current error.
   */

  val taskSteps: List<TaskStep>
) : Serializable {
  /**
   * The text of an error report to send to technical support, consisting of the body, attributes,
   * and task steps.
   */

  val report get() =
    listOf(this.body, this.reportAttributes, this.reportTaskSteps)
      .filterNot { part -> part.isEmpty() }
      .joinToString("\n\n")

  /**
   * The attributes, formatted as a string for use in an error report.
   */

  val reportAttributes get() =
    this.attributes
      .map { entry -> "${entry.key}:\n${entry.value}" }
      .joinToString("\n\n")

  /**
   * The task steps, formatted as a string for use in an error report.
   */

  val reportTaskSteps get() =
    this.taskSteps
      .mapIndexed { index, step ->
        val icon = if (step.resolution is TaskStepResolution.TaskStepFailed) "❌" else "✔️"
        val stepNumber = index + 1
        val description = step.description
        val resolution = step.resolution.message

        "$stepNumber. $description\n$icon $resolution"
      }
      .joinToString("\n\n")
      .let {
        if (it.isNotEmpty()) "Steps:\n$it" else it
      }
}
