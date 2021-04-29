package org.nypl.simplified.navigation.api

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified C : Any> ComponentActivity.navControllers(
  viewModelStoreOwner: ViewModelStoreOwner = this,
  interfaceType: Class<C> = C::class.java,
): Lazy<(C) -> Unit> = NavigationControllerLazy(viewModelStoreOwner, interfaceType)

inline fun <reified C : Any> Fragment.navControllers(
  viewModelStoreOwner: ViewModelStoreOwner = this,
  interfaceType: Class<C> = C::class.java,
): Lazy<(C) -> Unit> = NavigationControllerLazy(viewModelStoreOwner, interfaceType)

class NavigationControllerLazy<C : Any>(
  private val viewModelStoreOwner: ViewModelStoreOwner,
  private val interfaceType: Class<C>
) : Lazy<(C) -> Unit> {

  private lateinit var cached: (C) -> Unit

  override val value: (C) -> Unit
    get() =
      if (this::cached.isInitialized) {
        this.cached
      } else {
        NavigationControllerFinder.findController(viewModelStoreOwner, interfaceType)
          .also { this.cached = it }
      }

  override fun isInitialized(): Boolean =
    this::cached.isInitialized
}
