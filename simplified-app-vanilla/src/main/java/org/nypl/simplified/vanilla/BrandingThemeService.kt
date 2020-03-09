package org.nypl.simplified.vanilla

import org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.ui.theme.ThemeValue

class BrandingThemeService : BrandingThemeOverrideServiceType {
  override fun overrideTheme(): ThemeValue {
    return ThemeValue(
      name = "vanilla",
      colorLight = R.color.vanilla_deep_purple_primary_light,
      colorDark = R.color.vanilla_deep_purple_primary_dark,
      color = R.color.vanilla_deep_purple_primary,
      themeWithActionBar = R.style.VanillaTheme_ActionBar_DeepPurple,
      themeWithNoActionBar = R.style.VanillaTheme_NoActionBar_DeepPurple)
  }
}
