package org.nypl.simplified.accessibility

/**
 * An interface that allows for showing toasts.
 */

interface AccessibilityToastsType {

  /**
   * Show a toast with the given message.
   */

  fun show(message: String)
}
