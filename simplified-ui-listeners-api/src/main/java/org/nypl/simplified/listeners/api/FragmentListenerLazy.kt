package org.nypl.simplified.listeners.api

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified E : Any> ComponentActivity.fragmentListeners(
  viewModelStoreOwner: ViewModelStoreOwner = this,
  interfaceType: Class<E> = E::class.java,
): Lazy<FragmentListenerType<E>> = FragmentListenerLazy(viewModelStoreOwner, interfaceType)

inline fun <reified E : Any> Fragment.fragmentListeners(
  viewModelStoreOwner: ViewModelStoreOwner = this,
  interfaceType: Class<E> = E::class.java,
): Lazy<FragmentListenerType<E>> = FragmentListenerLazy(viewModelStoreOwner, interfaceType)

class FragmentListenerLazy<E : Any>(
  private val viewModelStoreOwner: ViewModelStoreOwner,
  private val interfaceType: Class<E>
) : Lazy<FragmentListenerType<E>> {

  private lateinit var cached: FragmentListenerType<E>

  override val value: FragmentListenerType<E>
    get() =
      if (this::cached.isInitialized) {
        this.cached
      } else {
        FragmentListenerFinder.findListener(viewModelStoreOwner, interfaceType)
          .also { this.cached = it }
      }

  override fun isInitialized(): Boolean =
    this::cached.isInitialized
}
