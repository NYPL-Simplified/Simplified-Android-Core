package org.nypl.simplified.listeners.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

abstract class ListenerRepositoryFactory<E : Any, S : Any>(
  private val fallbackFactory: ViewModelProvider.Factory
) : ViewModelProvider.Factory {

  abstract val initialState: S

  /**
   * Register the capabilities of the repository here.
   */

  abstract fun onListenerRepositoryCreated(repository: ListenerRepository<E, S>)

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    when {
      modelClass.isAssignableFrom(ListenerRepository::class.java) ->
        ListenerRepository<E, S>(initialState)
          .also(this::onListenerRepositoryCreated)
          as T
      else ->
        fallbackFactory.create(modelClass)
    }
}
