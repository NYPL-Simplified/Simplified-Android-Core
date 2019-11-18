package org.nypl.simplified.ui.screen

/**
 * Information about the device screen.
 */

interface ScreenSizeInformationType {

  /**
   * @param dp A size in dp
   *
   * @return The given size converted to pixels
   */

  fun dpToPixels(dp: Int): Double

  /**
   * @return `true` if the screen is currently in portrait orientation
   */

  val isPortrait: Boolean

  /**
   * @return The DPI of the current screen
   */

  val dpi: Double

  /**
   * @return The height of the screen in pixels
   */

  val heightPixels: Int

  /**
   * @return The width of the screen in pixels
   */

  val widthPixels: Int

}
