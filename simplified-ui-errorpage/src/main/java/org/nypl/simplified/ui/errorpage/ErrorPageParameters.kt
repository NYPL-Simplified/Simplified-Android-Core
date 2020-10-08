package org.nypl.simplified.ui.errorpage

import org.nypl.simplified.taskrecorder.api.TaskStep
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
) : Serializable
