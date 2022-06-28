package org.nypl.simplified.viewer.audiobook.ui.navigation

import androidx.compose.runtime.Composable
import org.nypl.simplified.viewer.audiobook.ui.screens.ContentsScreen
import org.nypl.simplified.viewer.audiobook.ui.screens.ContentsScreenListener
import org.nypl.simplified.viewer.audiobook.ui.screens.ErrorScreen
import org.nypl.simplified.viewer.audiobook.ui.screens.LoadingScreen
import org.nypl.simplified.viewer.audiobook.ui.screens.PlayerScreen
import org.nypl.simplified.viewer.audiobook.ui.screens.PlayerScreenListener
import org.nypl.simplified.viewer.audiobook.ui.screens.PlayerScreenState
import org.readium.r2.shared.publication.Link

internal sealed class Screen {

  @Composable
  abstract fun Screen()

  object Loading : Screen() {

    @Composable
    override fun Screen() {
      LoadingScreen()
    }
  }

  class Error(
    private val exception: Throwable
  ) : Screen() {

    @Composable
    override fun Screen() {
      ErrorScreen(exception)
    }
  }

  class Player(
    val state: PlayerScreenState,
    private val listener: PlayerScreenListener
  ) : Screen() {

    @Composable
    override fun Screen() {
      PlayerScreen(state, listener)
    }
  }

  class Contents(
    private val links: List<Link>,
    private val listener: ContentsScreenListener
  ) : Screen() {

    @Composable
    override fun Screen() {
      ContentsScreen(links, listener)
    }
  }
}
