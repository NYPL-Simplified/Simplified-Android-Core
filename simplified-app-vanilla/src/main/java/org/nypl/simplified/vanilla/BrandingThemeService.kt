package org.nypl.simplified.vanilla

import org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.ui.theme.ThemeValue

class BrandingThemeService : BrandingThemeOverrideServiceType {
  override fun overrideTheme(): ThemeValue {
    return ThemeValue(
      name = "vanilla",
      color = R.color.colorPrimary,
      colorLight = R.color.colorPrimaryLight,
      colorDark = R.color.colorPrimaryDark,
      themeWithActionBar = R.style.VanillaTheme_ActionBar,
      themeWithNoActionBar = R.style.VanillaTheme_NoActionBar
    )
  }
}
