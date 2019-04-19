package org.nypl.simplified.branding

import org.nypl.simplified.theme.ThemeValue

/**
 * A service that provides overrides for the application theme.
 */

interface BrandingThemeOverrideServiceType {

  /**
   * Provide a theme for the application. This theme will be used in preference
   * to account-specific colors.
   */

  fun overrideTheme(): ThemeValue

}
