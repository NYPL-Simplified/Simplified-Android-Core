package org.nypl.simplified.viewer.audiobook.ui.navigation

import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nypl.simplified.viewer.audiobook.ui.screens.ContentsScreenListener
import org.nypl.simplified.viewer.audiobook.ui.screens.PlayerScreenListener
import org.nypl.simplified.viewer.audiobook.ui.screens.PlayerScreenState
import org.readium.r2.shared.publication.Link

internal class Listener
  : PlayerScreenListener, ContentsScreenListener {

  private val backstack: Backstack<Screen> =
    Backstack(Screen.Loading)

  private val playerState: PlayerScreenState
    get() = backstack.screens
      .filterIsInstance(Screen.Player::class.java)
      .first().state

  val onBackPressedCallback = object : OnBackPressedCallback(false) {

    private val coroutineScope = MainScope()

    init {
        backstack.current
          .onEach { isEnabled = backstack.size > 1 }
          .launchIn(coroutineScope)
    }

    override fun handleOnBackPressed() {
      if (backstack.size > 1) {
        backstack.pop()
      }
    }
  }

  val currentScreen: StateFlow<Screen>
    get() = backstack.current

  fun onPlayerReady(state: PlayerScreenState) {
    backstack.replace(Screen.Player(state, this))
  }

  fun onLoadingException(error: Throwable) {
    backstack.replace(Screen.Error(error))
  }

  override fun onOpenToc() {
    val links = playerState.readingOrder
    val contents = Screen.Contents(links, this)
    backstack.add(contents)
  }

  override fun onTocItemCLicked(link: Link) {
    playerState.go(link)
    playerState.play()
    backstack.pop()
  }
}
