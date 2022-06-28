package org.nypl.simplified.viewer.audiobook.navigation

import androidx.compose.runtime.Composable
import org.nypl.simplified.viewer.audiobook.screens.ContentsScreen
import org.nypl.simplified.viewer.audiobook.screens.ContentsScreenListener
import org.nypl.simplified.viewer.audiobook.screens.ErrorScreen
import org.nypl.simplified.viewer.audiobook.screens.LoadingScreen
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreen
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenListener
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenState
import org.readium.r2.shared.publication.Link

internal sealed class AudioBookPlayerScreen {

  @Composable
  abstract fun Screen()

  object Loading : AudioBookPlayerScreen() {

    @Composable
    override fun Screen() {
      LoadingScreen()
    }
  }

  class Error(
    private val exception: Throwable
  ) : AudioBookPlayerScreen() {

    @Composable
    override fun Screen() {
      ErrorScreen(exception)
    }
  }

  class Player(
    val state: PlayerScreenState,
    private val listener: PlayerScreenListener
  ) : AudioBookPlayerScreen() {

    @Composable
    override fun Screen() {
      PlayerScreen(state, listener)
    }
  }

  class Contents(
    private val links: List<Link>,
    private val listener: ContentsScreenListener
  ) : AudioBookPlayerScreen() {

    @Composable
    override fun Screen() {
      ContentsScreen(links, listener)
    }
  }
}
