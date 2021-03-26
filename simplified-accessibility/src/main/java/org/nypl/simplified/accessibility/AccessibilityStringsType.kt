package org.nypl.simplified.accessibility

/**
 * An interface providing accessibility strings.
 */

interface AccessibilityStringsType {
  fun bookHasDownloaded(title: String): String
  fun bookIsDownloading(title: String): String
  fun bookIsOnHold(title: String): String
  fun bookReturned(title: String): String
  fun bookFailedReturn(title: String): String
  fun bookFailedLoan(title: String): String
  fun bookFailedDownload(title: String): String
}
