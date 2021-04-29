package org.nypl.simplified.navigation.api

import androidx.lifecycle.ViewModelProvider

private val PROVIDER_KEY: String =
  "org.nypl.simplified.navigation.NavigationViewModel.provider.key"

/**
 * Obtain access to the navigation ViewModel directly associated with the given viewModelStoreOwner.
 */

internal fun <T: Any> ViewModelProvider.getNavigation(): NavigationViewModel<T> {
  @Suppress("UNCHECKED_CAST")
  return this.get(PROVIDER_KEY, NavigationViewModel::class.java) as NavigationViewModel<T>
}
