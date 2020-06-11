package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import android.view.ActionMode
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepFailed
import org.nypl.simplified.taskrecorder.api.TaskStepResolution.TaskStepSucceeded
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.slf4j.LoggerFactory

class LifeActivity : AppCompatActivity() {

  private val logger = LoggerFactory.getLogger(LifeActivity::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logger.debug("onCreate")

    this.setContentView(R.layout.fragment_host)

    val blueFragment = BlueFragment()

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.fragmentHolder, blueFragment, "blue")
      .commit()
  }

  override fun onStart() {
    super.onStart()
    logger.debug("onStart")
  }

  override fun onStop() {
    super.onStop()
    logger.debug("onStop")
  }

  override fun onPause() {
    super.onPause()
    logger.debug("onPause")
  }
}
