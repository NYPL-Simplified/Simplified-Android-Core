package org.nypl.simplified.app

//Using a hard-coded color template map until Library Registry
//implementation eliminates the need for this hack.
class ThemeMatcher {
  companion object {
    fun actionBarStyle(colorSwatch: String): Int {
      return when(colorSwatch) {
        "Red" -> R.style.Simplified_RedTheme
        "Pink" -> R.style.Simplified_PinkTheme
        "Purple" -> R.style.Simplified_PurpleTheme
        "DeepPurple" -> R.style.Simplified_DeepPurpleTheme
        "Indigo" -> R.style.Simplified_IndigoTheme
        "Blue" -> R.style.Simplified_BlueTheme
        "LightBlue" -> R.style.Simplified_LightBlueTheme
        "Cyan" -> R.style.Simplified_LightBlueTheme
        "Teal" -> R.style.Simplified_TealTheme
        "Amber" -> R.style.Simplified_AmberTheme
        "Orange" -> R.style.Simplified_OrangeTheme
        "DeepOrange" -> R.style.Simplified_DeepOrangeTheme
        "Brown" -> R.style.Simplified_BrownTheme
        "Grey" -> R.style.Simplified_GreyTheme
        "BlueGrey" -> R.style.Simplified_BlueGreyTheme
        else -> R.style.Simplified_RedTheme
      }
    }

    fun noActionBarStyle(colorSwatch: String): Int {
      return when(colorSwatch) {
        "Red" -> R.style.Simplified_RedTheme_NoActionBar
        "Pink" -> R.style.Simplified_PinkTheme_NoActionBar
        "Purple" -> R.style.Simplified_PurpleTheme_NoActionBar
        "DeepPurple" -> R.style.Simplified_DeepPurpleTheme_NoActionBar
        "Indigo" -> R.style.Simplified_IndigoTheme_NoActionBar
        "Blue" -> R.style.Simplified_BlueTheme_NoActionBar
        "LightBlue" -> R.style.Simplified_LightBlueTheme_NoActionBar
        "Cyan" -> R.style.Simplified_LightBlueTheme_NoActionBar
        "Teal" -> R.style.Simplified_TealTheme_NoActionBar
        "Amber" -> R.style.Simplified_AmberTheme_NoActionBar
        "Orange" -> R.style.Simplified_OrangeTheme_NoActionBar
        "DeepOrange" -> R.style.Simplified_DeepOrangeTheme_NoActionBar
        "Brown" -> R.style.Simplified_BrownTheme_NoActionBar
        "Grey" -> R.style.Simplified_GreyTheme_NoActionBar
        "BlueGrey" -> R.style.Simplified_BlueGreyTheme_NoActionBar
        else -> R.style.Simplified_RedTheme_NoActionBar
      }
    }

    fun color(colorSwatch: String): Int {
      return when(colorSwatch) {
        "Red" -> R.color.red_primary_dark
        "Pink" -> R.color.pink_primary_dark
        "Purple" -> R.color.purple_primary_dark
        "DeepPurple" ->R.color.deep_purple_primary_dark
        "Indigo" -> R.color.indigo_primary_dark
        "Blue" -> R.color.blue_primary_dark
        "LightBlue" -> R.color.light_blue_primary_dark
        "Cyan" -> R.color.cyan_primary_dark
        "Teal" -> R.color.teal_primary_dark
        "Amber" -> R.color.amber_primary_dark
        "Orange" -> R.color.orange_primary_dark
        "DeepOrange" -> R.color.deep_orange_primary_dark
        "Brown" -> R.color.brown_primary_dark
        "Grey" -> R.color.grey_primary_dark
        "BlueGrey" -> R.color.blue_grey_primary_dark
        else -> R.color.app_primary_color
      }
    }
  }
}
