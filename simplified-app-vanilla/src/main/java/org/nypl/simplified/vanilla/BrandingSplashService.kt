package org.nypl.simplified.vanilla

import org.nypl.simplified.ui.branding.BrandingSplashServiceType

/**
 * A splash service for the vanilla app.
 */

class BrandingSplashService : BrandingSplashServiceType {
  override fun splashImageResource(): Int {
    return R.drawable.main_splash
  }

  override fun splashImageTitleResource(): Int {
    return R.drawable.main_splash_title
  }

  override val shouldShowLibrarySelectionScreen: Boolean =
    true
}
