package org.nypl.simplified.viewer.audiobook

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.librarysimplified.services.api.Services
import org.readium.navigator.media2.ExperimentalMedia2

/**
 * The activity for playing audio books.
 */

@OptIn(ExperimentalMedia2::class)
class AudioBookPlayerActivity : AppCompatActivity() {

  private val parameters: AudioBookPlayerParameters by lazy { AudioBookPlayerContract.parseIntent(intent) }
  private val services by lazy { Services.serviceDirectory() }
  private val viewModelFactory = { AudioBookPlayerViewModel.Factory(application, parameters, services) }
  private val viewModel: AudioBookPlayerViewModel by viewModels(factoryProducer = viewModelFactory)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    onBackPressedDispatcher.addCallback(this) {
      val shouldFinish = viewModel.onBackstackPressed()
      if (shouldFinish) {
        finish()
      }
    }

    setContent {
      val screen by viewModel.currentScreen.collectAsState()
      screen.Screen()
    }
  }
}
