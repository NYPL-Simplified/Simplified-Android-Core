package org.nypl.simplified.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import org.slf4j.LoggerFactory

/**
 * The default implementation of the [AccessibilityEventsType] interface that publishes
 * events.
 */

class AccessibilityEvents(context: Context) : AccessibilityEventsType {

  private val logger =
    LoggerFactory.getLogger(AccessibilityEvents::class.java)

  private val accessibilityManager =
    context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
  private val packageName =
    context.packageName

  private fun isSpokenFeedbackEnabled(): Boolean {
    return this.accessibilityManager
      .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
      .isNotEmpty()
  }

  override val spokenFeedbackEnabled: Boolean
    get() = this.isSpokenFeedbackEnabled()

  override fun show(message: String) {
    this.logger.debug("show: {}", message)
    if (this.spokenFeedbackEnabled) {
      val event = AccessibilityEvent.obtain()
      event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
      event.className = AccessibilityEvents::class.java.canonicalName
      event.packageName = this.packageName
      event.text.add(message)
      this.accessibilityManager.sendAccessibilityEvent(event)
    }
  }
}
