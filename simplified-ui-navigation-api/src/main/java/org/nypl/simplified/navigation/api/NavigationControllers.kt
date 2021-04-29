package org.nypl.simplified.navigation.api

import androidx.fragment.app.Fragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Functions to obtain access to navigation controllers.
 */

internal object NavigationControllers {

  val PROVIDER_KEY: String =
    "org.nypl.simplified.navigation.NavigationViewModel.provider.key"


  /**
   * Obtain access to the navigation controller directory associated with the given activity.
   */

  fun <T: Any> findNavigationViewModel(
    viewModelStoreOwner: ViewModelStoreOwner
  ): NavigationViewModel<T> {
    @Suppress("UNCHECKED_CAST")
    return ViewModelProvider(viewModelStoreOwner)
      .get(PROVIDER_KEY, NavigationViewModel::class.java)
      as NavigationViewModel<T>
  }

  /**
   * Obtain access to the navigation controller associated with the given activity.
   */

  fun <T : Any> findCommandSender(
    viewModelStoreOwner: ViewModelStoreOwner,
    interfaceType: Class<T>
  ): (T) -> Unit {

    if (viewModelStoreOwner is HasDefaultViewModelProviderFactory &&
      viewModelStoreOwner.defaultViewModelProviderFactory is NavigationAwareViewModelFactory<*>) {

      val navigationViewModel =
        ViewModelProvider(viewModelStoreOwner)
          .get(PROVIDER_KEY, NavigationViewModel::class.java)

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

    return this.findCommandSender(parentStoreOwner, interfaceType)
  }
}
