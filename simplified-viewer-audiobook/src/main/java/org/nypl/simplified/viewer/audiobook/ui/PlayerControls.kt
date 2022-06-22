package org.nypl.simplified.viewer.audiobook.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nypl.simplified.viewer.audiobook.R

@Composable
fun PlayerControls(
  modifier: Modifier,
  showPause: Boolean,
  onSkipForward: () -> Unit,
  onSkipBackward: () -> Unit,
  onPlay: () -> Unit,
  onPause: () -> Unit,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(35.dp)
  ) {
    BackwardButton(onSkipBackward)
    PlayPauseButton(onPlay, onPause, showPause)
    ForwardButton(onSkipForward)
  }
}

@Composable
private fun PlayPauseButton(
  onPlay: () -> Unit,
  onPause: () -> Unit,
  showPause: Boolean
) {
  ControlButton(
    onClick = if (showPause) onPause else onPlay,
    iconResourceId = if (showPause) R.drawable.pause_icon else R.drawable.play_icon,
    overlay = null,
    contentDescription = if (showPause) "Pause" else "Play"
  )
}

@Composable
private fun BackwardButton(
  onClick: () -> Unit,
) {
  ControlButton(
    onClick = onClick,
    iconResourceId = R.drawable.circle_arrow_backward,
    overlay = { Text(text = "15", fontWeight = FontWeight.Bold) },
    contentDescription = "Go backward"
  )
}

@Composable
private fun ForwardButton(
  onClick: () -> Unit,
) {
  ControlButton(
    onClick = onClick,
    iconResourceId = R.drawable.circle_arrow_forward,
    overlay = { Text(text = "15", fontWeight = FontWeight.Bold) },
    contentDescription = "Go forward"
  )
}

@Composable
private fun ControlButton(
  onClick: () -> Unit,
  @DrawableRes iconResourceId: Int,
  overlay: (@Composable () -> Unit)?,
  contentDescription: String?
) {
  IconButton(
    onClick = onClick
  ) {
    Icon(
      modifier = Modifier.size(48.dp),
      painter = painterResource(id = iconResourceId),
      contentDescription = contentDescription
    )
    overlay?.let {
      it()
    }
  }
}
