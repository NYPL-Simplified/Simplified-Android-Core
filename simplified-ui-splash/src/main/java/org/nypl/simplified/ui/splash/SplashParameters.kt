package org.nypl.simplified.ui.splash

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import java.io.File
import java.io.Serializable

/**
 * Parameters for the splash screen.
 */

data class SplashParameters(

    /**
   * The text color for the splash screen.
   */

  @ColorInt val textColor: Int,

    /**
   * The background color for the splash screen.
   */

  @ColorInt val background: Int,

    /**
   * The image resource for the splash screen image.
   */

  @DrawableRes val splashImageResource: Int,

    /**
   * The number of seconds to keep the splash screen image visible.
   */

  val splashImageSeconds: Long,

    /**
   * The email address to which migration reports will be sent.
   */

  val splashMigrationReportEmail: String? = null) : Serializable
