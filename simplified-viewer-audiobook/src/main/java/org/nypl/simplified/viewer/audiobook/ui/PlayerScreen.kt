package org.nypl.simplified.viewer.audiobook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.nypl.simplified.viewer.audiobook.ui.PlayerControls
import org.nypl.simplified.viewer.audiobook.ui.PlayerProgression
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@Composable
fun AudioBookPlayerLoadingScreen() {
  Box(Modifier.fillMaxSize()) {
    CircularProgressIndicator(
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
fun AudioBookPlayerFailureScreen(exception: Throwable) {
  Box(Modifier.fillMaxSize()) {
    Text(
      modifier = Modifier.align(Alignment.Center),
      text = exception.message ?: "Failed to load audiobook."
    )
  }
}

@OptIn(ExperimentalMedia2::class, ExperimentalTime::class)
@Composable
fun AudioBookPlayerReadyScreen(
  audioBookPlayerState: PlayerScreenState
) {
  when (val error = audioBookPlayerState.error.value) {
    null -> AudioBookPlayerScreen(audioBookPlayerState)
    else -> AudioBookPlayerFailureScreen(error)
  }
}

@OptIn(ExperimentalTime::class, ExperimentalMedia2::class)
@Composable
fun AudioBookPlayerScreen(
  audioBookPlayerState: PlayerScreenState
) {
  val resource = audioBookPlayerState.resource.value
  val paused = audioBookPlayerState.paused.value

  Column(
    modifier = Modifier
      .padding(25.dp)
      .fillMaxSize(),
    verticalArrangement = Arrangement.SpaceEvenly,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    PlayerMetadata(
      modifier = Modifier,
      title = audioBookPlayerState.title,
      author = audioBookPlayerState.author
    )

    PlayerProgression(
      modifier = Modifier.fillMaxWidth(),
      position = resource.position,
      duration = checkNotNull(resource.duration) { "Unknown duration" },
      onPositionChange = { audioBookPlayerState.seek(it) }
    )

    PlayerControls(
      modifier = Modifier,
      showPause = !paused,
      onSkipBackward = audioBookPlayerState::goBackward,
      onSkipForward = audioBookPlayerState::goForward,
      onPlay = audioBookPlayerState::play,
      onPause = audioBookPlayerState::pause
    )
  }
}

@Composable
fun PlayerMetadata(
  modifier: Modifier,
  title: String,
  author: String?
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = title, fontWeight = FontWeight.Bold)
    author?.let { author ->
      Text(text = author)
    }
  }
}

@Composable
fun PlayerCover(

) {

}
