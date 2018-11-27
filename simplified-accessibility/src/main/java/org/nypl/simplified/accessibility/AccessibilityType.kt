package org.nypl.simplified.accessibility

import android.app.Activity

/**
 * The interface to accessibility features.
 */

interface AccessibilityType {

  /**
   * @return `true` if a screen reader is currently enabled
   */

  val isScreenReaderEnabled : Boolean

  /**
   * Announce the given text to the screen reader, if one is enabled.
   */

  fun announce(message: String)
}
