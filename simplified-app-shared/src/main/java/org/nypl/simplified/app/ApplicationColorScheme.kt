package org.nypl.simplified.app

import android.support.annotation.ColorInt
import java.io.Serializable

/**
 * The details of the application's color theme.
 */

data class ApplicationColorScheme(

  /**
   * The name of the main color.
   */

  val name: String,

  /**
   * The main color as a packed RGBA integer.
   */

  @ColorInt val  colorRGBA: Int,

  /**
   * The resource ID of the theme to use when an action bar is required.
   */

  val activityThemeResourceWithActionBar: Int,

  /**
   * The resource ID of the theme to use when an action bar is not required.
   */

  val activityThemeResourceWithoutActionBar: Int): Serializable
