package org.nypl.simplified.ui.branding

import org.nypl.simplified.ui.theme.ThemeValue

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
