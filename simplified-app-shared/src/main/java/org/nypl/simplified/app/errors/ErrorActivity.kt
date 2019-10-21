package org.nypl.simplified.app.errors

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.ui.errorpage.ErrorPageBaseActivity
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

class ErrorActivity : ErrorPageBaseActivity() {

  companion object {

    private const val PARAMETER_ID =
      "org.nypl.simplified.ui.errorpage.ErrorPageBaseActivity.parameters"

    /**
     * Start a new error activity.
     *
     * @param from The parent activity
     * @param parameters The error page parameters
     */

    fun <T : PresentableErrorType> startActivity(
      from: Activity,
      parameters: ErrorPageParameters<T>
    ) {
      val b = Bundle()
      b.putSerializable(this.PARAMETER_ID, parameters)
      val i = Intent(from, ErrorActivity::class.java)
      i.putExtras(b)
      from.startActivity(i)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    this.setTheme(
      Simplified.getServices()
        .currentTheme
        .themeWithActionBar)

    super.onCreate(savedInstanceState)
  }
}
