package org.nypl.simplified.viewer.audiobook.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun PlayerProgression(
  modifier: Modifier,
  position: Duration,
  duration: Duration,
  onPositionChange: (Duration) -> Unit
) {
  var slidingPosition by remember { mutableStateOf<Duration?>(null) }
  val displayedPosition = slidingPosition ?: position

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    ProgressionBar(
      modifier = Modifier,
      position = displayedPosition,
      duration = duration,
      onValueChange = {
        slidingPosition = it
      },
      onValueChangeFinished = {
        val newPosition = checkNotNull(slidingPosition)
        slidingPosition = null
        onPositionChange(newPosition)
      }
    )

    ProgressionText(
      position = displayedPosition,
      duration = duration
    )
  }
}

@Composable
private fun ProgressionBar(
  modifier: Modifier,
  position: Duration,
  duration: Duration,
  onValueChange: (Duration) -> Unit,
  onValueChangeFinished: () -> Unit
) {
  Slider(
    value = position.inWholeSeconds.toFloat(),
    modifier = modifier,
    valueRange = 0f..duration.inWholeSeconds.toFloat(),
    enabled = true,
    steps = 0,
    onValueChangeFinished = onValueChangeFinished,
    onValueChange = { onValueChange(it.toDouble().seconds) }
  )
}

@Composable
private fun ProgressionText(
  position: Duration,
  duration: Duration
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = position.formatElapsedTime(), fontWeight = FontWeight.Bold
    )
    Text(
      text = duration.formatElapsedTime()
    )
  }
}

private fun Duration.formatElapsedTime(): String =
  DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))
