package org.nypl.simplified.viewer.audiobook

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import org.librarysimplified.services.api.Services
import org.readium.navigator.media2.ExperimentalMedia2
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The activity for playing audio books.
 */

@OptIn(ExperimentalMedia2::class)
class AudioBookPlayerActivity : AppCompatActivity() {

  private val logger: Logger = LoggerFactory.getLogger(AudioBookPlayerActivity::class.java)
  private val parameters: AudioBookPlayerParameters by lazy { AudioBookPlayerContract.parseIntent(intent) }
  private val services by lazy { Services.serviceDirectory() }
  private val viewModelFactory = { AudioBookPlayerViewModel.Factory(application, parameters, services) }
  private val viewModel: AudioBookPlayerViewModel by viewModels(factoryProducer = viewModelFactory)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    onBackPressedDispatcher.addCallback(this) {
      viewModel.popBackstack()
    }

    setContent {
      viewModel.currentScreen.value.Screen()
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    if (isFinishing) {
      viewModel.closeNavigator()
    }
  }
}
