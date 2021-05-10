package org.nypl.simplified.main

sealed class MainActivityListenedEvent {

  data class SplashEvent(
    val event: org.nypl.simplified.ui.splash.SplashEvent
  ) : MainActivityListenedEvent()

  data class OnboardingEvent(
    val event: org.nypl.simplified.ui.onboarding.OnboardingEvent
  ) : MainActivityListenedEvent()

  data class MainFragmentEvent(
    val event: org.nypl.simplified.main.MainFragmentEvent
  ) : MainActivityListenedEvent()

  data class ProfileSelectionEvent(
    val event: org.nypl.simplified.ui.profiles.ProfileSelectionEvent
  ) : MainActivityListenedEvent()

  data class ProfileModificationEvent(
    val event: org.nypl.simplified.ui.profiles.ProfileModificationEvent
  ) : MainActivityListenedEvent()
}
