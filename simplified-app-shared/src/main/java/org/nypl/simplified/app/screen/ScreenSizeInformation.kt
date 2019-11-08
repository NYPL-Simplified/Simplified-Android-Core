package org.nypl.simplified.app.screen

import android.content.res.Resources
import org.nypl.simplified.app.ScreenSizeInformationType
import org.slf4j.LoggerFactory

/**
 * The default implementation of the [ScreenSizeInformationType] interface.
 */

class ScreenSizeInformation(
  private val resources: Resources
) : ScreenSizeInformationType {

  private val logger = LoggerFactory.getLogger(ScreenSizeInformation::class.java)

  init {
    val dm = this.resources.displayMetrics
    val dpHeight = dm.heightPixels.toFloat() / dm.density
    val dpWidth = dm.widthPixels.toFloat() / dm.density
    this.logger.debug("screen ({} x {})", dpWidth, dpHeight)
    this.logger.debug("screen ({} x {})", dm.widthPixels, dm.heightPixels)
  }

  override fun screenDPToPixels(
    dp: Int
  ): Double {
    val scale = this.resources.displayMetrics.density
    return (dp * scale).toDouble() + 0.5
  }

  override fun screenGetDPI(): Double {
    return this.resources.displayMetrics.densityDpi.toDouble()
  }

  override fun screenGetHeightPixels(): Int {
    return this.resources.displayMetrics.heightPixels
  }

  override fun screenGetWidthPixels(): Int {
    return this.resources.displayMetrics.widthPixels
  }
}
