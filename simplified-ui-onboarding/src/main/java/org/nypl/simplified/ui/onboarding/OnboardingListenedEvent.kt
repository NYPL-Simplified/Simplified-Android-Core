package org.nypl.simplified.ui.onboarding

sealed class OnboardingListenedEvent {

  data class OnboardingStartScreenEvent(
    val event: org.nypl.simplified.ui.onboarding.OnboardingStartScreenEvent
  ) : OnboardingListenedEvent()

  data class AccountListRegistryEvent(
    val event: org.nypl.simplified.ui.accounts.AccountListRegistryEvent
  ) : OnboardingListenedEvent()
}
