package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepFailed
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepSucceeded
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

class ErrorPageActivity : AppCompatActivity(), ErrorPageListenerType {

  override fun onErrorPageSendReport(parameters: ErrorPageParameters<*>) {
  }

  private lateinit var errorFragment: ErrorPageFragment

  data class ExampleError(
    override val message: String
  ) : PresentableErrorType

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.error_host)

    val taskSteps =
      mutableListOf<TaskStep<ExampleError>>()
    taskSteps.add(
      TaskStep(
        "Rearranging furniture.",
        TaskStepSucceeded("Furniture successfully rearranged.")
      )
    )
    taskSteps.add(
      TaskStep(
        "Shampooing carpet.",
        TaskStepSucceeded("Carpet successfully cleaned.")
      )
    )
    taskSteps.add(
      TaskStep(
        "Sorting cornflakes alphabetically.",
        TaskStepSucceeded("Cornflakes successfully filed under 'C'.")
      )
    )
    taskSteps.add(
      TaskStep(
        "Ironing clothes.",
        TaskStepFailed(
          "Ironing failed: The ironing board did not survive the attempt.",
          ExampleError("Ouch"), Exception()
        )
      )
    )

    val attributes =
      sortedMapOf(
        Pair("Address", "http://www.example.com"),
        Pair("Library", "06924cf8-b360-4c88-ab45-1ae727df22dc"),
        Pair("Another", "Something else")
      )

    this.errorFragment =
      ErrorPageFragment.create(
        ErrorPageParameters(
          emailAddress = "someone@example.com",
          body = "An error occurred.",
          subject = "[simplified] Error",
          attributes = attributes,
          taskSteps = taskSteps
        )
      )

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.errorHolder, this.errorFragment, "ERROR_MAIN")
      .commit()
  }
}
