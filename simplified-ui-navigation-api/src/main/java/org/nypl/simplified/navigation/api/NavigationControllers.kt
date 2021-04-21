package org.nypl.simplified.navigation.api

import androidx.fragment.app.Fragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Functions to obtain access to navigation controllers.
 */

object NavigationControllers {

  val PROVIDER_KEY: String =
    "org.nypl.simplified.navigation.NavigationControllerViewModel.provider.key"


  /**
   * Obtain access to the navigation controller directory associated with the given activity.
   */

  fun <T: NavigationControllerViewModel> findViewModel(
    viewModelStoreOwner: ViewModelStoreOwner
  ): T {
    @Suppress("UNCHECKED_CAST")
    return ViewModelProvider(viewModelStoreOwner)
      .get(PROVIDER_KEY, NavigationControllerViewModel::class.java)
      as T
  }

  /**
   * Obtain access to the navigation controller associated with the given activity.
   */

  fun <T : NavigationControllerType> find(
    viewModelStoreOwner: ViewModelStoreOwner,
    interfaceType: Class<T>
  ): T {

    if (viewModelStoreOwner is HasDefaultViewModelProviderFactory &&
      viewModelStoreOwner.defaultViewModelProviderFactory is NavigationAwareViewModelFactory<*>) {

      val navigationViewModel =
        ViewModelProvider(viewModelStoreOwner)
          .get(PROVIDER_KEY, NavigationControllerViewModel::class.java)

      navigationViewModel
        .navigationControllerIfAvailable(interfaceType)
        ?.let { return it }
    }

    if (viewModelStoreOwner !is Fragment) {
      throw IllegalArgumentException(
        "No navigation controllers of type $interfaceType are available"
      )
    }

    val parentStoreOwner =
        viewModelStoreOwner.parentFragment ?: viewModelStoreOwner.requireActivity()

    return this.find(parentStoreOwner, interfaceType)
  }
}
