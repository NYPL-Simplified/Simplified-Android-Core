package org.nypl.simplified.viewer.audiobook.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nypl.simplified.viewer.audiobook.ui.components.PlayerBottomBar
import org.nypl.simplified.viewer.audiobook.ui.components.PlayerControls
import org.nypl.simplified.viewer.audiobook.ui.components.PlayerProgression
import org.readium.navigator.media2.ExperimentalMedia2
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

internal interface PlayerScreenListener {

  fun onOpenToc()
}

@Composable
internal fun PlayerScreen(
  audioBookPlayerState: PlayerScreenState,
  listener: PlayerScreenListener
) {
  Scaffold(
    bottomBar = {
      PlayerBottomBar(
        onSpeedClicked = {},
        onSleepClicked = {},
        onChaptersClicked = listener::onOpenToc
      )
    },
    content = { paddingValues ->
      Box(
        modifier = Modifier
          .padding(paddingValues),
        propagateMinConstraints = true
      ) {
        PlayerContent(
          modifier = Modifier
            .padding(20.dp)
            .fillMaxSize(),
          audioBookPlayerState = audioBookPlayerState
        )
      }
    }
  )
}

@OptIn(ExperimentalTime::class, ExperimentalMedia2::class)
@Composable
internal fun PlayerContent(
  modifier: Modifier,
  audioBookPlayerState: PlayerScreenState,
) {
  val resource = audioBookPlayerState.resource.value
  val paused = audioBookPlayerState.paused.value

  Column(
    modifier = modifier,
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
      duration = resource.duration ?: Duration.ZERO,
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
internal fun PlayerMetadata(
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
internal fun PlayerCover(

) {

}
