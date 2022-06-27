package org.nypl.simplified.viewer.audiobook.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.nypl.simplified.viewer.audiobook.R
import org.nypl.simplified.viewer.audiobook.util.CenteredOverlay
import org.nypl.simplified.viewer.audiobook.util.asTextUnit

@Composable
internal fun PlayerBottomBar(
  onSpeedClicked: () -> Unit,
  onSleepClicked: () -> Unit,
  onChaptersClicked: () -> Unit
) {
  BottomAppBar {
    SpeedButton(onSleepClicked)
    SleepButton(onSleepClicked)
    ChaptersButton(onChaptersClicked)

  }
}

@Composable
private fun (RowScope).SpeedButton(
  onClick: () -> Unit
) {
  MenuItem(
    onClick = onClick,
    label = "Speed",
    iconResourceId = R.drawable.speed,
    contentDescription = "Speed",
    overlay = {
      Text(
        text = "1.0x",
        fontSize = 7.dp.asTextUnit(),
        textAlign = TextAlign.Center
      )
    }
  )
}

@Composable
private fun (RowScope).SleepButton(
  onClick: () -> Unit
) {
  MenuItem(
    onClick = onClick,
    label = "Sleep",
    iconResourceId = R.drawable.sleep_icon,
    contentDescription = "Sleep"
  )
}

@Composable
private fun (RowScope).ChaptersButton(
  onClick: () -> Unit
) {
  MenuItem(
    onClick = onClick,
    label = "Chapters",
    iconResourceId = R.drawable.list,
    contentDescription = "Chapters"
  )
}

@Composable
private fun (RowScope).MenuItem(
  onClick: () -> Unit,
  label: String,
  @DrawableRes iconResourceId: Int,
  contentDescription: String?,
  overlay: (@Composable () -> Unit)? = null,
) {
  BottomNavigationItem(
    selected = false,
    onClick = onClick,
    label = { Text(label) },
    icon = {
      CenteredOverlay(overlay = overlay) {
        Icon(
          modifier = Modifier.size(24.dp),
          painter = painterResource(id = iconResourceId),
          contentDescription = contentDescription
        )
      }
    }
  )
}
