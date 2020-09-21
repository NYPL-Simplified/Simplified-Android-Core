package org.nypl.simplified.ui.errorpage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.reports.Reports

/**
 * A convenient base activity used to show error pages.
 */

abstract class ErrorPageBaseActivity : AppCompatActivity(), ErrorPageListenerType {

  companion object {
    const val PARAMETER_ID =
      "org.nypl.simplified.ui.errorpage.ErrorPageBaseActivity.parameters"
  }

  private lateinit var parameters: ErrorPageParameters
  private lateinit var errorFragment: ErrorPageFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.error_host)

    val currentIntent =
      this.intent
        ?: throw IllegalStateException("No intent provided for activity")
    val currentExtras =
      currentIntent.extras
        ?: throw IllegalStateException("No extras provided for activity")
    val currentParameters =
      currentExtras.getSerializable(PARAMETER_ID)
        ?: throw IllegalStateException("No parameters provided for activity")

    this.parameters =
      if (currentParameters is ErrorPageParameters) {
        currentParameters
      } else {
        throw IllegalStateException("Parameters of wrong type provided for activity")
      }

    this.errorFragment = ErrorPageFragment.create(this.parameters)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.errorHolder, this.errorFragment, "ERROR_MAIN")
      .commit()
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters) {
    Reports.sendReportsDefault(
      context = this,
      address = parameters.emailAddress,
      subject = parameters.subject,
      body = parameters.body
    )
  }
}
