package org.nypl.simplified.app

class ThemeMatcher {
  companion object {
    fun actionBarStyle(colorSwatch: String): Int {
      when(colorSwatch) {
        "Red" -> return R.style.Simplified_RedTheme
        "Pink" -> return R.style.Simplified_PinkTheme
        "Purple" -> return R.style.Simplified_PurpleTheme
        "DeepPurple" -> return R.style.Simplified_DeepPurpleTheme
        "Indigo" -> return R.style.Simplified_IndigoTheme
        "Blue" -> return R.style.Simplified_BlueTheme
        "LightBlue" -> return R.style.Simplified_LightBlueTheme
        "Cyan" -> return R.style.Simplified_LightBlueTheme
        "Teal" -> return R.style.Simplified_TealTheme
        "Amber" -> return R.style.Simplified_AmberTheme
        "Orange" -> return R.style.Simplified_OrangeTheme
        "DeepOrange" -> return R.style.Simplified_DeepOrangeTheme
        "Brown" -> return R.style.Simplified_BrownTheme
        "Grey" -> return R.style.Simplified_GreyTheme
        "BlueGrey" -> return R.style.Simplified_BlueGreyTheme
        else -> return R.style.Simplified_RedTheme
      }
    }

    fun noActionBarStyle(colorSwatch: String): Int {
      when(colorSwatch) {
        "Red" -> return R.style.Simplified_RedTheme_NoActionBar
        "Pink" -> return R.style.Simplified_PinkTheme_NoActionBar
        "Purple" -> return R.style.Simplified_PurpleTheme_NoActionBar
        "DeepPurple" -> return R.style.Simplified_DeepPurpleTheme_NoActionBar
        "Indigo" -> return R.style.Simplified_IndigoTheme_NoActionBar
        "Blue" -> return R.style.Simplified_BlueTheme_NoActionBar
        "LightBlue" -> return R.style.Simplified_LightBlueTheme_NoActionBar
        "Cyan" -> return R.style.Simplified_LightBlueTheme_NoActionBar
        "Teal" -> return R.style.Simplified_TealTheme_NoActionBar
        "Amber" -> return R.style.Simplified_AmberTheme_NoActionBar
        "Orange" -> return R.style.Simplified_OrangeTheme_NoActionBar
        "DeepOrange" -> return R.style.Simplified_DeepOrangeTheme_NoActionBar
        "Brown" -> return R.style.Simplified_BrownTheme_NoActionBar
        "Grey" -> return R.style.Simplified_GreyTheme_NoActionBar
        "BlueGrey" -> return R.style.Simplified_BlueGreyTheme_NoActionBar
        else -> return R.style.Simplified_RedTheme_NoActionBar
      }
    }
  }
}
