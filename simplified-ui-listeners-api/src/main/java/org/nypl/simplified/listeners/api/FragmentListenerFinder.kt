package org.nypl.simplified.listeners.api

import androidx.fragment.app.Fragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

object FragmentListenerFinder {

  fun <E : Any> findListener(
    viewModelStoreOwner: ViewModelStoreOwner,
    interfaceType: Class<E>,
    recursive: Boolean = true
  ): FragmentListenerType<E> {
    if (viewModelStoreOwner is HasDefaultViewModelProviderFactory &&
      viewModelStoreOwner.defaultViewModelProviderFactory is ListenerRepositoryFactory<*, *>
    ) {
      ViewModelProvider(viewModelStoreOwner)
        .get(ListenerRepository::class.java)
        .getListener(interfaceType.kotlin)
        ?.let { return it }
    }

    if (viewModelStoreOwner !is Fragment || !recursive) {
      throw IllegalArgumentException(
        "No listeners of type $interfaceType are available"
      )
    }

    val parentStoreOwner =
      viewModelStoreOwner.parentFragment ?: viewModelStoreOwner.requireActivity()

    return this.findListener(parentStoreOwner, interfaceType)
  }
}
