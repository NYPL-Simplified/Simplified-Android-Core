package org.nypl.simplified.ui.branding

/**
 * A service that provides a splash screen image.
 */

interface BrandingSplashServiceType {

  /**
   * Provide a splash image resource for the application.
   */

  @androidx.annotation.DrawableRes
  fun splashImageResource(): Int

  /**
   * Provide a splash image title resource for the application.
   */

  @androidx.annotation.DrawableRes
  fun splashImageTitleResource(): Int

  /**
   * Should the splash screen show the library selection screen?
   */

  val shouldShowLibrarySelectionScreen: Boolean
}
