package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accessibility.AccessibilityToastsType

class MockAccessibilityToasts : AccessibilityToastsType {

  val messages = mutableListOf<String>()

  override fun show(message: String) {
    this.messages.add(message)
  }
}
