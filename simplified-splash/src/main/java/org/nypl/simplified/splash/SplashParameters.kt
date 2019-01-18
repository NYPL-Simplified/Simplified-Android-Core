package org.nypl.simplified.splash

import android.support.annotation.ColorInt
import java.io.Serializable

data class SplashParameters(
  @ColorInt val textColor: Int,
  @ColorInt val background: Int,
  val splashImageResource: Int,
  val splashImageSeconds: Long) : Serializable
