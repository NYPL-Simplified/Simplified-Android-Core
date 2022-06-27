package org.nypl.simplified.viewer.audiobook.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
internal fun Dp.asTextUnit(): TextUnit =
  with(LocalDensity.current) {
    val textSize = value / fontScale
    textSize.sp
  }

@Composable
internal fun CenteredOverlay(
  overlay: (@Composable () -> Unit)?,
  content: @Composable () -> Unit
) {
  Box(
    contentAlignment = Alignment.Center
  ) {
    content()
    overlay?.invoke()
  }
}
