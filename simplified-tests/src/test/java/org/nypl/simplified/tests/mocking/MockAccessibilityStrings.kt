package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accessibility.AccessibilityStringsType

class MockAccessibilityStrings : AccessibilityStringsType {
  override fun bookHasDownloaded(title: String): String {
    return "bookHasDownloaded $title"
  }
  override fun bookIsDownloading(title: String): String {
    return "bookIsDownloading $title"
  }
}
