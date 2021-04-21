package org.nypl.simplified.navigation.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NavigationAwareViewModelFactory<T : NavigationControllerViewModel>(
  private val navigationViewModelClass: Class<T>,
  private val fallbackFactory: ViewModelProvider.Factory
) : ViewModelProvider.NewInstanceFactory() {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    when {
      modelClass.isAssignableFrom(NavigationControllerViewModel::class.java) ->
        navigationViewModelClass.getDeclaredConstructor().newInstance() as T
      else ->
        fallbackFactory.create(modelClass)
    }
}
