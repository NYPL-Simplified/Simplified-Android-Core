package org.nypl.simplified.viewer.audiobook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreen
import org.nypl.simplified.viewer.audiobook.screens.ContentsScreen
import org.nypl.simplified.viewer.audiobook.screens.ContentsScreenListener
import org.nypl.simplified.viewer.audiobook.screens.ErrorScreen
import org.nypl.simplified.viewer.audiobook.screens.LoadingScreen
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenListener
import org.nypl.simplified.viewer.audiobook.screens.PlayerScreenState
import org.readium.r2.shared.publication.Link

internal class AudioBookPlayerListener
  : PlayerScreenListener, ContentsScreenListener {

  sealed class Screen {

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

  private class Backstack(
    initialScreen: Screen
  ) {

    val screens: MutableList<Screen> =
      mutableListOf(initialScreen)

    val current: State<Screen>
      get() = currentMutableState

    private val currentMutableState: MutableState<Screen> =
      mutableStateOf(initialScreen)

    fun push(screen: Screen) {
      screens.add(screen)
      currentMutableState.value = screen
    }

    fun pop() {
      screens.removeLast()
      currentMutableState.value = screens.last()
    }
  }

  val currentScreen: State<Screen>
    get() = backstack.current

  private val backstack: Backstack =
    Backstack(Screen.Loading)

  private val playerState: PlayerScreenState
    get() = backstack.screens
      .filterIsInstance(Screen.Player::class.java)
      .first().state

  fun openPlayer(state: PlayerScreenState) {
    backstack.push(Screen.Player(state, this))
  }

  fun openError(error: Throwable) {
    backstack.push(Screen.Error(error))
  }

  fun popBackstack() {
    backstack.pop()
  }

  override fun onOpenToc() {
    val links = playerState.readingOrder
    val contents = Screen.Contents(links, this)
    backstack.push(contents)
  }

  override fun onTocItemCLicked(link: Link) {
    backstack.pop()
    playerState.go(link)
    playerState.play()
  }
}
