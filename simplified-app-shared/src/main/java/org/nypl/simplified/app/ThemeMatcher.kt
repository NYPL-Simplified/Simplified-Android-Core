package org.nypl.simplified.app

//Using a hard-coded color template map until Library Registry
//implementation eliminates the need for this hack.
class ThemeMatcher {
  companion object {
    fun actionBarStyle(colorSwatch: String): Int {
      val lowerCaseColor = colorSwatch.toLowerCase()
      return when(lowerCaseColor) {
        "red" -> R.style.Simplified_RedTheme
        "pink" -> R.style.Simplified_PinkTheme
        "purple" -> R.style.Simplified_PurpleTheme
        "deeppurple" -> R.style.Simplified_DeepPurpleTheme
        "indigo" -> R.style.Simplified_IndigoTheme
        "blue" -> R.style.Simplified_BlueTheme
        "lightblue" -> R.style.Simplified_LightBlueTheme
        "cyan" -> R.style.Simplified_LightBlueTheme
        "teal" -> R.style.Simplified_TealTheme
        "amber" -> R.style.Simplified_AmberTheme
        "orange" -> R.style.Simplified_OrangeTheme
        "deeporange" -> R.style.Simplified_DeepOrangeTheme
        "brown" -> R.style.Simplified_BrownTheme
        "grey" -> R.style.Simplified_GreyTheme
        "bluegrey" -> R.style.Simplified_BlueGreyTheme
        else -> R.style.Simplified_RedTheme
      }
    }

    fun noActionBarStyle(colorSwatch: String): Int {
      val lowerCaseColor = colorSwatch.toLowerCase()
      return when(lowerCaseColor) {
        "red" -> R.style.Simplified_RedTheme_NoActionBar
        "pink" -> R.style.Simplified_PinkTheme_NoActionBar
        "purple" -> R.style.Simplified_PurpleTheme_NoActionBar
        "deeppurple" -> R.style.Simplified_DeepPurpleTheme_NoActionBar
        "indigo" -> R.style.Simplified_IndigoTheme_NoActionBar
        "blue" -> R.style.Simplified_BlueTheme_NoActionBar
        "lightblue" -> R.style.Simplified_LightBlueTheme_NoActionBar
        "cyan" -> R.style.Simplified_LightBlueTheme_NoActionBar
        "teal" -> R.style.Simplified_TealTheme_NoActionBar
        "amber" -> R.style.Simplified_AmberTheme_NoActionBar
        "orange" -> R.style.Simplified_OrangeTheme_NoActionBar
        "deeporange" -> R.style.Simplified_DeepOrangeTheme_NoActionBar
        "brown" -> R.style.Simplified_BrownTheme_NoActionBar
        "grey" -> R.style.Simplified_GreyTheme_NoActionBar
        "bluegrey" -> R.style.Simplified_BlueGreyTheme_NoActionBar
        else -> R.style.Simplified_RedTheme_NoActionBar
      }
    }

    fun color(colorSwatch: String): Int {
      val lowerCaseColor = colorSwatch.toLowerCase()
      return when(lowerCaseColor) {
        "red" -> R.color.red_primary_dark
        "pink" -> R.color.pink_primary_dark
        "purple" -> R.color.purple_primary_dark
        "deeppurple" ->R.color.deep_purple_primary_dark
        "indigo" -> R.color.indigo_primary_dark
        "blue" -> R.color.blue_primary_dark
        "lightblue" -> R.color.light_blue_primary_dark
        "cyan" -> R.color.cyan_primary_dark
        "teal" -> R.color.teal_primary_dark
        "amber" -> R.color.amber_primary_dark
        "orange" -> R.color.orange_primary_dark
        "deeporange" -> R.color.deep_orange_primary_dark
        "brown" -> R.color.brown_primary_dark
        "grey" -> R.color.grey_primary_dark
        "bluegrey" -> R.color.blue_grey_primary_dark
        else -> R.color.app_primary_color
      }
    }
  }
}
