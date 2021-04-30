package org.nypl.simplified.listeners.api

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified E : Any, S : Any> ComponentActivity.listenerRepositories(
  viewModelStoreOwner: ViewModelStoreOwner = this
): Lazy<ListenerRepository<E, S>> = ListenerRepositoryLazy(viewModelStoreOwner)

inline fun <reified E : Any, S : Any> Fragment.listenerRepositories(
  viewModelStoreOwner: ViewModelStoreOwner = this
): Lazy<ListenerRepository<E, S>> = ListenerRepositoryLazy(viewModelStoreOwner)

class ListenerRepositoryLazy<E : Any, S : Any>(
  private val viewModelStoreOwner: ViewModelStoreOwner,
) : Lazy<ListenerRepository<E, S>> {

  private lateinit var cached: ListenerRepository<E, S>

  override val value: ListenerRepository<E, S>
    get() =
      if (this::cached.isInitialized) {
        this.cached
      } else {
        @Suppress("UNCHECKED_CAST")
        this.cached =
          ViewModelProvider(viewModelStoreOwner).get(ListenerRepository::class.java)
          as ListenerRepository<E, S>

        this.cached
      }

  override fun isInitialized(): Boolean =
    this::cached.isInitialized
}
