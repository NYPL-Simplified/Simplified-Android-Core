package org.nypl.simplified.app

class ThemeMatcher {
  companion object {
    fun style(colorSwatch: String): Int {
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
        "Lime" -> return R.style.Simplified_LimeTheme
        "Yellow" -> return R.style.Simplified_YellowTheme
        "Amber" -> return R.style.Simplified_AmberTheme
        "Orange" -> return R.style.Simplified_OrangeTheme
        "DeepOrange" -> return R.style.Simplified_DeepOrangeTheme
        "Brown" -> return R.style.Simplified_BrownTheme
        "Grey" -> return R.style.Simplified_GreyTheme
        "BlueGrey" -> return R.style.Simplified_BlueGreyTheme
        else -> return R.style.Simplified_RedTheme
      }
    }
  }

  fun noActionBarStyle(colorSwatch: String): Int {
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
      "Lime" -> return R.style.Simplified_LimeTheme
      "Yellow" -> return R.style.Simplified_YellowTheme
      "Amber" -> return R.style.Simplified_AmberTheme
      "Orange" -> return R.style.Simplified_OrangeTheme
      "DeepOrange" -> return R.style.Simplified_DeepOrangeTheme
      "Brown" -> return R.style.Simplified_BrownTheme
      "Grey" -> return R.style.Simplified_GreyTheme
      "BlueGrey" -> return R.style.Simplified_BlueGreyTheme
      else -> return R.style.Simplified_RedTheme
    }
  }
}
