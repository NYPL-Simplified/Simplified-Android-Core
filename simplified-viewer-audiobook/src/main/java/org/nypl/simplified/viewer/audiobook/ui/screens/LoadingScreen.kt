package org.nypl.simplified.viewer.audiobook.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun LoadingScreen() {
  Box(Modifier.fillMaxSize()) {
    LinearProgressIndicator(
      modifier = Modifier.align(Alignment.Center)
    )
  }
}
