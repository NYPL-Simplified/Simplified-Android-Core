package org.nypl.simplified.viewer.audiobook.navigation

import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nypl.simplified.viewer.audiobook.screens.ContentsScreenListener
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenListener
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenState
import org.nypl.simplified.viewer.audiobook.util.Backstack
import org.readium.r2.shared.publication.Link

internal class AudioBookPlayerListener
  : PlayerScreenListener, ContentsScreenListener {

  private val backstack: Backstack<AudioBookPlayerScreen> =
    Backstack(AudioBookPlayerScreen.Loading)

  private val playerState: PlayerScreenState
    get() = backstack.screens
      .filterIsInstance(AudioBookPlayerScreen.Player::class.java)
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

  val currentScreen: StateFlow<AudioBookPlayerScreen>
    get() = backstack.current

  fun onPlayerReady(state: PlayerScreenState) {
    backstack.replace(AudioBookPlayerScreen.Player(state, this))
  }

  fun onLoadingException(error: Throwable) {
    backstack.replace(AudioBookPlayerScreen.Error(error))
  }

  override fun onOpenToc() {
    val links = playerState.readingOrder
    val contents = AudioBookPlayerScreen.Contents(links, this)
    backstack.add(contents)
  }

  override fun onTocItemCLicked(link: Link) {
    playerState.go(link)
    playerState.play()
    backstack.pop()
  }
}
