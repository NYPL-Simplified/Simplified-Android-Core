package org.nypl.simplified.accessibility

import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import android.widget.Toast

/**
 * The default implementation of the {@link AccessibilityType} interface.
 */

class Accessibility private constructor(private val context: Context) : AccessibilityType {

  private fun checkEnabled(): Boolean {
    val am = this.context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.isTouchExplorationEnabled
  }

  override val isScreenReaderEnabled: Boolean
    get() = checkEnabled()

  override fun announce(message: String) {
    if (isScreenReaderEnabled) {
      this.runOnUIThread(Runnable {
        val toast = Toast.makeText(this.context, message, Toast.LENGTH_LONG)
        toast.show()
      })
    }
  }

  private fun runOnUIThread(r: Runnable) {
    val looper = Looper.getMainLooper()
    val handler = Handler(looper)
    handler.post(r)
  }

  companion object {

    /**
     * Create a new accessibility interface.
     *
     * @param context The application context
     */

    fun create(context: Context): AccessibilityType {
      return Accessibility(context)
    }
  }
}