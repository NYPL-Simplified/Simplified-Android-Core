package org.nypl.simplified.accessibility

import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.view.accessibility.AccessibilityManager

/**
 * An implementation of the {@link AccessibilityType} interface that does nothing.
 */

class AccessibilityNoOp private constructor(private val context: Context) : AccessibilityType {

  private fun checkEnabled(): Boolean {
    val am = this.context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.isTouchExplorationEnabled
  }

  override val isScreenReaderEnabled: Boolean
    get() = checkEnabled()

  override fun announce(message: String) {

  }

  companion object {

    /**
     * Create a new accessibility interface.
     *
     * @param context The application context
     */

    fun create(context: Context): AccessibilityType {
      return AccessibilityNoOp(context)
    }
  }
}