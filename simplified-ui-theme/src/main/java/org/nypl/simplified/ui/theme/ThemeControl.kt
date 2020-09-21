package org.nypl.simplified.ui.theme

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes

class ThemeControl {

  companion object {

    // Automatically generated: See matcher.sh
    @JvmStatic
    val themeValues =
      listOf(
        ThemeValue(
          name = "amber",
          colorLight = R.color.simplified_material_amber_primary_light,
          colorDark = R.color.simplified_material_amber_primary_dark,
          color = R.color.simplified_material_amber_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Amber,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Amber
        ),
        ThemeValue(
          name = "blue",
          colorLight = R.color.simplified_material_blue_primary_light,
          colorDark = R.color.simplified_material_blue_primary_dark,
          color = R.color.simplified_material_blue_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Blue,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Blue
        ),
        ThemeValue(
          name = "blue_grey",
          colorLight = R.color.simplified_material_blue_grey_primary_light,
          colorDark = R.color.simplified_material_blue_grey_primary_dark,
          color = R.color.simplified_material_blue_grey_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_BlueGrey,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_BlueGrey
        ),
        ThemeValue(
          name = "brown",
          colorLight = R.color.simplified_material_brown_primary_light,
          colorDark = R.color.simplified_material_brown_primary_dark,
          color = R.color.simplified_material_brown_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Brown,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Brown
        ),
        ThemeValue(
          name = "cyan",
          colorLight = R.color.simplified_material_cyan_primary_light,
          colorDark = R.color.simplified_material_cyan_primary_dark,
          color = R.color.simplified_material_cyan_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Cyan,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Cyan
        ),
        ThemeValue(
          name = "deep_orange",
          colorLight = R.color.simplified_material_deep_orange_primary_light,
          colorDark = R.color.simplified_material_deep_orange_primary_dark,
          color = R.color.simplified_material_deep_orange_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_DeepOrange,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_DeepOrange
        ),
        ThemeValue(
          name = "deep_purple",
          colorLight = R.color.simplified_material_deep_purple_primary_light,
          colorDark = R.color.simplified_material_deep_purple_primary_dark,
          color = R.color.simplified_material_deep_purple_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_DeepPurple,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_DeepPurple
        ),
        ThemeValue(
          name = "green",
          colorLight = R.color.simplified_material_green_primary_light,
          colorDark = R.color.simplified_material_green_primary_dark,
          color = R.color.simplified_material_green_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Green,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Green
        ),
        ThemeValue(
          name = "grey",
          colorLight = R.color.simplified_material_grey_primary_light,
          colorDark = R.color.simplified_material_grey_primary_dark,
          color = R.color.simplified_material_grey_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Grey,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Grey
        ),
        ThemeValue(
          name = "indigo",
          colorLight = R.color.simplified_material_indigo_primary_light,
          colorDark = R.color.simplified_material_indigo_primary_dark,
          color = R.color.simplified_material_indigo_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Indigo,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Indigo
        ),
        ThemeValue(
          name = "light_blue",
          colorLight = R.color.simplified_material_light_blue_primary_light,
          colorDark = R.color.simplified_material_light_blue_primary_dark,
          color = R.color.simplified_material_light_blue_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_LightBlue,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_LightBlue
        ),
        ThemeValue(
          name = "orange",
          colorLight = R.color.simplified_material_orange_primary_light,
          colorDark = R.color.simplified_material_orange_primary_dark,
          color = R.color.simplified_material_orange_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Orange,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Orange
        ),
        ThemeValue(
          name = "pink",
          colorLight = R.color.simplified_material_pink_primary_light,
          colorDark = R.color.simplified_material_pink_primary_dark,
          color = R.color.simplified_material_pink_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Pink,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Pink
        ),
        ThemeValue(
          name = "purple",
          colorLight = R.color.simplified_material_purple_primary_light,
          colorDark = R.color.simplified_material_purple_primary_dark,
          color = R.color.simplified_material_purple_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Purple,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Purple
        ),
        ThemeValue(
          name = "red",
          colorLight = R.color.simplified_material_red_primary_light,
          colorDark = R.color.simplified_material_red_primary_dark,
          color = R.color.simplified_material_red_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Red,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Red
        ),
        ThemeValue(
          name = "teal",
          colorLight = R.color.simplified_material_teal_primary_light,
          colorDark = R.color.simplified_material_teal_primary_dark,
          color = R.color.simplified_material_teal_primary,
          themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Teal,
          themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Teal
        )
      )

    @JvmStatic
    val themeFallback =
      ThemeValue(
        name = "red",
        colorLight = R.color.simplified_material_red_primary_light,
        colorDark = R.color.simplified_material_red_primary_dark,
        color = R.color.simplified_material_red_primary,
        themeWithActionBar = R.style.SimplifiedTheme_ActionBar_Red,
        themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_Red
      )

    @JvmStatic
    val themesByName =
      themeValues.map { value -> Pair(value.name, value) }
        .toMap()

    @JvmStatic
    @StyleRes
    fun actionBarStyle(colorSwatch: String): Int =
      (themesByName[colorSwatch.toLowerCase()] ?: themeFallback).themeWithActionBar

    @JvmStatic
    @StyleRes
    fun noActionBarStyle(colorSwatch: String): Int =
      (themesByName[colorSwatch.toLowerCase()] ?: themeFallback).themeWithNoActionBar

    @JvmStatic
    @ColorRes
    fun color(colorSwatch: String): Int =
      (themesByName[colorSwatch.toLowerCase()] ?: themeFallback).colorDark

    @JvmStatic
    @ColorInt
    fun resolveColorAttribute(
      theme: Resources.Theme,
      @AttrRes attribute: Int
    ): Int {
      val typedValue = TypedValue()
      theme.resolveAttribute(attribute, typedValue, true)
      return typedValue.data
    }
  }
}
