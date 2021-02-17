package org.nypl.simplified.accessibility

import android.content.Context
import android.widget.Toast

/**
 * The default implementation of the [AccessibilityToastsType] interface that just displays
 * a toast using the given context.
 */

class AccessibilityToasts(
  private val context: Context
) : AccessibilityToastsType {
  override fun show(message: String) {
    Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
  }
}
