package org.nypl.simplified.accessibility

/**
 * The interface exposed by accessibility services.
 */

interface AccessibilityServiceType {

  /**
   * @return `true` if spoken feedback is enabled
   */

  val spokenFeedbackEnabled: Boolean
}
