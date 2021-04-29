package org.nypl.simplified.navigation.api

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified C: Any> ComponentActivity.navViewModels(
  viewModelStoreOwner: ViewModelStoreOwner = this
): Lazy<NavigationViewModel<C>> = NavigationViewModelLazy(viewModelStoreOwner)

inline fun <reified C: Any> Fragment.navViewModels(
  viewModelStoreOwner: ViewModelStoreOwner = this
): Lazy<NavigationViewModel<C>> = NavigationViewModelLazy(viewModelStoreOwner)


class NavigationViewModelLazy<C : Any>(
  private val viewModelStoreOwner: ViewModelStoreOwner,
) : Lazy<NavigationViewModel<C>> {

  private lateinit var cached: NavigationViewModel<C>

  override val value: NavigationViewModel<C>
    get() =
      if (this::cached.isInitialized) {
        this.cached
      } else {
        ViewModelProvider(viewModelStoreOwner).getNavigation<C>()
          .also { this.cached = it }
      }

  override fun isInitialized(): Boolean =
    this::cached.isInitialized
}
