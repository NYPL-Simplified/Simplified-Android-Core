package org.nypl.simplified.branding

/**
 * A service that provides a splash screen image.
 */

interface BrandingSplashServiceType {

  /**
   * Provide a splash image resource for the application.
   */

  @android.support.annotation.DrawableRes
  fun splashImageResource(): Int

}
