package org.nypl.simplified.ui.screen

import android.content.res.Resources

/**
 * The default implementation of the [ScreenSizeInformationType] interface.
 */

class ScreenSizeInformation(
  private val resources: Resources
) : ScreenSizeInformationType {

  override val isPortrait: Boolean
    get() = run {
      return this.resources.displayMetrics.heightPixels > this.resources.displayMetrics.widthPixels
    }

  override val dpi: Double
    get() = run {
      this.resources.displayMetrics.densityDpi.toDouble()
    }

  override val heightPixels: Int
    get() = run {
      this.resources.displayMetrics.heightPixels
    }

  override val widthPixels: Int
    get() = run {
      this.resources.displayMetrics.widthPixels
    }

  override fun dpToPixels(dp: Int): Double {
    val scale = this.resources.displayMetrics.density
    return (dp * scale).toDouble() + 0.5
  }
}
