package org.nypl.simplified.viewer.audiobook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
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
  audioBookPlayerState: AudioBookPlayerState
) {
  val playback = audioBookPlayerState.playback.collectAsState()

  when (playback.value.state) {
    MediaNavigator.Playback.State.Playing,
    MediaNavigator.Playback.State.Paused,
    MediaNavigator.Playback.State.Finished -> {
      AudioBookPlayerScreen(audioBookPlayerState, playback)
    }
    MediaNavigator.Playback.State.Error -> {
      val exception = Exception("An error occurred in the player.")
      AudioBookPlayerFailureScreen(exception)
    }
  }
}

@OptIn(ExperimentalTime::class, ExperimentalMedia2::class)
@Composable
fun AudioBookPlayerScreen(
  audioBookPlayerState: AudioBookPlayerState,
  playback: State<MediaNavigator.Playback>
) {
  Box(Modifier.fillMaxSize()) {
    PlayerControls(
      modifier = Modifier.align(Alignment.Center),
      onGoPrevious = audioBookPlayerState::goPrevious,
      onGoNext = audioBookPlayerState::goNext,
      onSkipBackward = audioBookPlayerState::goBackward,
      onSkipForward = audioBookPlayerState::goForward,
      onPlayPause = {
        when (playback.value.state) {
          MediaNavigator.Playback.State.Playing -> audioBookPlayerState.pause()
          MediaNavigator.Playback.State.Paused -> audioBookPlayerState.play()
          MediaNavigator.Playback.State.Finished -> audioBookPlayerState.play()
          MediaNavigator.Playback.State.Error -> {}
        }
      }
    )
  }
}

@Composable
fun PlayerControls(
  modifier: Modifier,
  onGoPrevious: () -> Unit,
  onGoNext: () -> Unit,
  onSkipForward: () -> Unit,
  onSkipBackward: () -> Unit,
  onPlayPause: () -> Unit
) {
  Column(modifier) {
    TextButton(onGoPrevious) {
      Text("Previous chapter")
    }

    TextButton(onSkipBackward) {
      Text("Skip backward")
    }

    TextButton(onPlayPause) {
      Text("Play/Pause")
    }

    TextButton(onSkipForward) {
      Text("Skip forward")
    }

    TextButton(onGoNext) {
      Text("Next chapter")
    }
  }
}

@OptIn(ExperimentalMedia2::class, ExperimentalTime::class)
class AudioBookPlayerState(
  private val mediaNavigator: MediaNavigator,
  private val navigatorScope: CoroutineScope
) {

  val playback: StateFlow<MediaNavigator.Playback>
    get() = mediaNavigator.playback

  fun goPrevious() = navigatorScope.launch {
    val currentIndex = playback.value.resource.index
    if (currentIndex > 0) {
      mediaNavigator.go(mediaNavigator.publication.readingOrder[currentIndex - 1])
    }
  }

  fun goNext() = navigatorScope.launch {
    val currentIndex = playback.value.resource.index
    if (currentIndex + 1 < mediaNavigator.publication.readingOrder.size) {
      mediaNavigator.go(mediaNavigator.publication.readingOrder[currentIndex + 1])
    }
  }

  fun play() = navigatorScope.launch {
      mediaNavigator.play()
  }

  fun pause() = navigatorScope.launch {
    mediaNavigator.pause()
  }

  fun goBackward() = navigatorScope.launch {
    mediaNavigator.goBackward()
  }

  fun goForward() = navigatorScope.launch {
    mediaNavigator.goForward()
  }
}
