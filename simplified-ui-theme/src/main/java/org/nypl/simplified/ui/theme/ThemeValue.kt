package org.nypl.simplified.ui.theme

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes

/**
 * A loaded theme.
 */

data class ThemeValue(

  /**
   * The name of the theme.
   */

  val name: String,

  /**
   * A lightened version of the primary color.
   */

  @ColorRes
  val colorLight: Int,

  /**
   * A darkened version of the primary color.
   */

  @ColorRes
  val colorDark: Int,

  /**
   * The primary color.
   */

  @ColorRes
  val color: Int,

  /**
   * The theme variant that contains an action bar.
   */

  @StyleRes
  val themeWithActionBar: Int,

  /**
   * The theme variant that does not contain an action bar.
   */

  @StyleRes
  val themeWithNoActionBar: Int
)
