package org.nypl.simplified.tests.mocking

import org.nypl.simplified.accessibility.AccessibilityStringsType

class MockAccessibilityStrings : AccessibilityStringsType {
  override fun bookHasDownloaded(title: String): String = "bookHasDownloaded $title"
  override fun bookIsDownloading(title: String): String = "bookIsDownloading $title"
  override fun bookIsOnHold(title: String): String = "bookIsOnHold $title"
  override fun bookReturned(title: String): String = "bookReturned $title"
  override fun bookFailedReturn(title: String): String = "bookFailedReturn $title"
  override fun bookFailedLoan(title: String): String = "bookFailedLoan $title"
  override fun bookFailedDownload(title: String): String = "bookFailedDownload $title"
}
