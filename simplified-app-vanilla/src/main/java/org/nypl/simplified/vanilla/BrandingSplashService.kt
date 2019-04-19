package org.nypl.simplified.vanilla

import org.nypl.simplified.branding.BrandingSplashServiceType

/**
 * A splash service for the vanilla app.
 */

class BrandingSplashService : BrandingSplashServiceType {
  override fun splashImageResource(): Int {
    return R.drawable.vanilla_splash
  }
}
