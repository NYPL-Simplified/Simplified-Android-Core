package org.nypl.simplified.main

import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.listeners.api.ListenerRepositoryFactory
import org.nypl.simplified.listeners.api.ListenerRepository
import org.nypl.simplified.ui.onboarding.OnboardingEvent
import org.nypl.simplified.ui.profiles.ProfileModificationEvent
import org.nypl.simplified.ui.profiles.ProfileSelectionEvent
import org.nypl.simplified.ui.splash.SplashEvent

class MainActivityDefaultViewModelFactory(fallbackFactory: ViewModelProvider.Factory) :
  ListenerRepositoryFactory<MainActivityListenedEvent, Unit>(fallbackFactory) {

  override val initialState: Unit = Unit

  override fun onListenerRepositoryCreated(repository: ListenerRepository<MainActivityListenedEvent, Unit>) {
    repository.registerListener(SplashEvent::class, MainActivityListenedEvent::SplashEvent)
    repository.registerListener(OnboardingEvent::class, MainActivityListenedEvent::OnboardingEvent)
    repository.registerListener(MainFragmentEvent::class, MainActivityListenedEvent::MainFragmentEvent)
    repository.registerListener(ProfileSelectionEvent::class, MainActivityListenedEvent::ProfileSelectionEvent)
    repository.registerListener(ProfileModificationEvent::class, MainActivityListenedEvent::ProfileModificationEvent)
  }
}
