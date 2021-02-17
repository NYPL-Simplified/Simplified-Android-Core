package org.nypl.simplified.accessibility

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * The interface exposed by accessibility services.
 */

interface AccessibilityServiceType : LifecycleObserver {

  /**
   * @return `true` if spoken feedback is enabled
   */

  val spokenFeedbackEnabled: Boolean

  /**
   * A view became available. This typically means that the activity to which this
   * service is to be attached has been started.
   */

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun onViewAvailable(owner: LifecycleOwner)

  /**
   * A view became unavailable. This typically means that the activity to which this
   * service is attached has been destroyed.
   */

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  fun onViewUnavailable(owner: LifecycleOwner)
}
