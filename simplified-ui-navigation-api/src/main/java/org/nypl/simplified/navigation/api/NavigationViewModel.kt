package org.nypl.simplified.navigation.api

import androidx.lifecycle.ViewModel

abstract class NavigationViewModel<C> : ViewModel() {

  abstract fun registerHandler(callback: (C) -> Unit)

  abstract fun unregisterHandler()

  abstract val navigationControllers: Map<Class<*>, Any>

  fun <T : Any> navigationControllerIfAvailable(
    navigationClass: Class<T>
  ): ((T) -> Unit)? {
    @Suppress("UNCHECKED_CAST")
    return this.navigationControllers[navigationClass] as? ((T) -> Unit)?
  }
}
