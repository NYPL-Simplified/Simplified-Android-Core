package org.nypl.simplified.viewer.audiobook.ui.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.nypl.simplified.viewer.audiobook.R
import org.nypl.simplified.viewer.audiobook.ui.util.CenteredOverlay
import org.nypl.simplified.viewer.audiobook.ui.util.asTextUnit

@Composable
internal fun PlayerControls(
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
    overlay = { SkipTextOverlay() } ,
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
    overlay = { SkipTextOverlay() },
    contentDescription = "Go forward"
  )
}

@Composable
private fun SkipTextOverlay() {
  Text(
    text = "15",
    fontWeight = FontWeight.Bold,
    fontSize = 14.dp.asTextUnit(),
    textAlign = TextAlign.Center
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
    CenteredOverlay(overlay = overlay) {
      Icon(
        modifier = Modifier.size(48.dp),
        painter = painterResource(id = iconResourceId),
        contentDescription = contentDescription
      )
    }
  }
}
