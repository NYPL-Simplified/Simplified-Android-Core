package org.nypl.labs.OpenEbooks.app

import org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.ui.theme.ThemeValue

class OEIBrandingThemeService : BrandingThemeOverrideServiceType {
  override fun overrideTheme(): ThemeValue {
    return ThemeValue(
      name = "openEbooks",
      color = R.color.oeColorPrimary,
      colorLight = R.color.oeColorAccent,
      colorDark = R.color.oeColorPrimaryDark,
      themeWithActionBar = R.style.OEI_ActionBar,
      themeWithNoActionBar = R.style.OEI_NoActionBar
    )
  }
}
