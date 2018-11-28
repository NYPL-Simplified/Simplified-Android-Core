package org.nypl.simplified.app.player

import android.support.annotation.ColorInt
import java.io.Serializable

/**
 * Parameters for the audio book player.
 */

data class AudioBookLoadingFragmentParameters(

  /**
   * The primary color used to tint various views.
   */

  @ColorInt val primaryColor: Int) : Serializable
