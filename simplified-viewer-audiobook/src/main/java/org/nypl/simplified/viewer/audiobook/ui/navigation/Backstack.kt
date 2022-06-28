package org.nypl.simplified.viewer.audiobook.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A stack of screens which is never empty.
 */
internal class Backstack<T>(
  initialScreen: T
) {

  /**
   * All the screens in the stack, from bottom to top.
   */
  val screens: MutableList<T> =
    mutableListOf(initialScreen)

  /**
   * The screen currently at the top of the stack
   */
  val current: StateFlow<T>
    get() = currentMutableState

  /**
   * The number of screens in the stack
   */
  val size: Int
    get() = screens.size

  private val currentMutableState: MutableStateFlow<T> =
    MutableStateFlow(initialScreen)

  /**
   * Add a screen on to of the backstack
   */
  fun add(screen: T) {
    screens.add(screen)
    currentMutableState.value = screen
  }

  /**
   * Replace the current fragment on top of the backstack
   */
  fun replace (screen: T) {
    screens.removeLast()
    screens.add(screen)
    currentMutableState.value = screen
  }

  /**
   * Pop the top screen from the backstack.
   *
   * Nothing will happen if doing so would lead to an empty backstack.
   */
  fun pop() {
    if (size > 1) {
      screens.removeLast()
      currentMutableState.value = screens.last()
    }
  }
}
