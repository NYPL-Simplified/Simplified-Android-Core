package org.nypl.simplified.simplye

import org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.ui.theme.ThemeValue

/**
 * A theme service for the app.
 */

class SimplyEThemeService : BrandingThemeOverrideServiceType {
  override fun overrideTheme(): ThemeValue {
    return ThemeValue(
      name = "LFA",
      colorLight = R.color.simplified_material_red_primary_light,
      colorDark = R.color.simplified_material_red_primary_dark,
      color = R.color.simplified_material_red_primary,
      themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Red,
      themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Red)
  }
}