package org.nypl.simplified.accessibility

/**
 * An interface that allows for publishing accessibility events.
 */

interface AccessibilityEventsType {

  /**
   * @return `true` if spoken feedback is enabled
   */

  val spokenFeedbackEnabled: Boolean

  /**
   * Publish an event with the given message.
   */

  fun show(message: String)
}
