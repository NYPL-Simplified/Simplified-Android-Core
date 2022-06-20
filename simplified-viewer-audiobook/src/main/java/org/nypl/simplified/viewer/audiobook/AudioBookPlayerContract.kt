package org.nypl.simplified.viewer.audiobook

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf

object AudioBookPlayerContract {

  private const val PARAMETER_ID =
    "org.nypl.simplified.viewer.audiobook.AudioBookPlayerActivity.parameters"

  fun createIntent(context: Context, parameters: AudioBookPlayerParameters): Intent {
    val bundle = bundleOf(this.PARAMETER_ID to parameters)
    val intent = Intent(context, AudioBookPlayerActivity::class.java)
    intent.putExtras(bundle)
    return intent
  }

  fun parseIntent(intent: Intent): AudioBookPlayerParameters =
    intent.extras!!.getSerializable(PARAMETER_ID) as AudioBookPlayerParameters
}

