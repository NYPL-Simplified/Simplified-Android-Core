package org.nypl.simplified.accessibility

import android.content.res.Resources

/**
 * The default implementation of the [AccessibilityStringsType].
 */

class AccessibilityStrings(
  private val resources: Resources
) : AccessibilityStringsType {

  override fun bookHasDownloaded(title: String): String {
    return this.resources.getString(R.string.bookHasDownloaded, title)
  }

  override fun bookIsDownloading(title: String): String {
    return this.resources.getString(R.string.bookIsDownloading, title)
  }
}
