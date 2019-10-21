package org.nypl.simplified.vanilla.with_profiles

import org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.ui.theme.ThemeValue

class BrandingThemeService : BrandingThemeOverrideServiceType {
  override fun overrideTheme(): ThemeValue {
    return ThemeValue(
      name = "vanilla",
      colorLight = R.color.vanilla_orange_primary_light,
      colorDark = R.color.vanilla_orange_primary_dark,
      color = R.color.vanilla_orange_primary,
      themeWithActionBar = R.style.VanillaTheme_ActionBar_Orange,
      themeWithNoActionBar = R.style.VanillaTheme_NoActionBar_Orange)
  }
}