package org.nypl.simplified.ui.theme

/**
 * Access to the current theme, taking into account any branding overrides.
 *
 * @see [org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType]
 */

interface ThemeServiceType {

  /**
   * Return the theme appropriate to the current profile, taking into account any branding
   * overrides that may be in effect.
   */

  fun findCurrentTheme(): ThemeValue

}