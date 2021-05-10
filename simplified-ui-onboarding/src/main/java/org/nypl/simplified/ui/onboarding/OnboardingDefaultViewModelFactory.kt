package org.nypl.simplified.ui.onboarding

import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.listeners.api.ListenerRepositoryFactory
import org.nypl.simplified.listeners.api.ListenerRepository
import org.nypl.simplified.ui.accounts.AccountListRegistryEvent

class OnboardingDefaultViewModelFactory(fallbackFactory: ViewModelProvider.Factory) :
  ListenerRepositoryFactory<OnboardingListenedEvent, Unit>(fallbackFactory) {

  override val initialState: Unit = Unit

  override fun onListenerRepositoryCreated(repository: ListenerRepository<OnboardingListenedEvent, Unit>) {
    repository.registerListener(AccountListRegistryEvent::class, OnboardingListenedEvent::AccountListRegistryEvent)
    repository.registerListener(OnboardingStartScreenEvent::class, OnboardingListenedEvent::OnboardingStartScreenEvent)
  }
}
