package org.nypl.simplified.ui.onboarding

sealed class OnboardingStartScreenEvent {

  object FindLibrary : OnboardingStartScreenEvent()

  object AddLibraryLater : OnboardingStartScreenEvent()
}
