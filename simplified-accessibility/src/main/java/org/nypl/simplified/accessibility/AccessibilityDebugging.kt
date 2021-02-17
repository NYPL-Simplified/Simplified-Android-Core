package org.nypl.simplified.accessibility

/**
 * Methods to debug accessibility functionality.
 */

object AccessibilityDebugging {

  /**
   * If `alwaysShowToasts` is `true`, then toasts will _always_ be shown regardless of whether
   * or not a screen reader is running.
   */

  @Volatile
  var alwaysShowToasts: Boolean = false
}
