package org.nypl.simplified.ui.accounts

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Generic ViewModel factory to pass in runtime arguments and restore SavedStateHandle.
 * @param owner
 * @param args initial arguments from parent. e.g: Fragment arguments
 * @param creator closure for creating ViewModel
 * */
class AssistedViewModelFactory<T : ViewModel>(
  owner: SavedStateRegistryOwner,
  args: Bundle?,
  private val creator: (SavedStateHandle) -> T
) : AbstractSavedStateViewModelFactory(owner, args) {
  override fun <T : ViewModel?> create(
    key: String,
    modelClass: Class<T>,
    handle: SavedStateHandle
  ): T = creator(handle) as T
}

/**
 * Extension function for Fragments to use AssistedViewModelFactory as a delegate.
 * */
inline fun <reified T : ViewModel> Fragment.assistedViewModels(crossinline creator: (SavedStateHandle) -> T): Lazy<T> {
  return viewModels { AssistedViewModelFactory(this, arguments) { creator(it) } }
}
