package org.nypl.simplified.simplye

import org.nypl.simplified.ui.branding.BrandingSplashServiceType

/**
 * A splash service for the SimplyE app.
 */

class BrandingSplashService : BrandingSplashServiceType {
  override val shouldShowLibrarySelectionScreen: Boolean = true
  override fun splashImageResource(): Int =
    R.drawable.simplified_splash
  override fun splashImageTitleResource(): Int =
    R.drawable.simplified_splash_title
}
