package org.nypl.simplified.navigation.api

import androidx.fragment.app.Fragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

object NavigationControllerFinder {

  fun <T : Any> findController(
    viewModelStoreOwner: ViewModelStoreOwner,
    interfaceType: Class<T>,
    recursive: Boolean = true
  ): (T) -> Unit {
    if (viewModelStoreOwner is HasDefaultViewModelProviderFactory &&
      viewModelStoreOwner.defaultViewModelProviderFactory is NavigationAwareViewModelFactory<*>
    ) {
      ViewModelProvider(viewModelStoreOwner).getNavigation<T>()
        .navigationControllerIfAvailable(interfaceType)
        ?.let { return it }
    }

    if (viewModelStoreOwner !is Fragment || !recursive) {
      throw IllegalArgumentException(
        "No navigation controllers of type $interfaceType are available"
      )
    }

    val parentStoreOwner =
      viewModelStoreOwner.parentFragment ?: viewModelStoreOwner.requireActivity()

    return this.findController(parentStoreOwner, interfaceType)
  }
}
