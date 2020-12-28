package org.nypl.labs.OpenEbooks.app

import org.nypl.simplified.ui.branding.BrandingSplashServiceType

/**
 * A splash service for the Open eBooks
 */

class OEIBrandingSplashService : BrandingSplashServiceType {
  override val shouldShowLibrarySelectionScreen: Boolean =
    false

  override fun splashImageResource(): Int {
    return R.drawable.oei_splash
  }

  override fun splashImageTitleResource(): Int {
    return R.drawable.oei_splash
  }
}
