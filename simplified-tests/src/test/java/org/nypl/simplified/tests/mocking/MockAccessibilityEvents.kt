package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accessibility.AccessibilityEventsType

class MockAccessibilityEvents : AccessibilityEventsType {

  val events = mutableListOf<String>()
  var spokenFeedback = false

  override val spokenFeedbackEnabled: Boolean
    get() = this.spokenFeedback

  override fun show(message: String) {
    this.events.add(message)
  }
}
