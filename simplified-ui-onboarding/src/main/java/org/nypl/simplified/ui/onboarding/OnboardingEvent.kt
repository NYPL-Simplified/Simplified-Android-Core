package org.nypl.simplified.ui.onboarding

sealed class OnboardingEvent {

  object OnboardingCompleted : OnboardingEvent()
}
