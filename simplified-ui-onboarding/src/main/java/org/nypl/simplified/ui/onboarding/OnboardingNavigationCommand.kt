package org.nypl.simplified.ui.onboarding

sealed class OnboardingNavigationCommand {

  sealed class AccountNavigationCommand : OnboardingNavigationCommand() {

    object OpenSettingsAccountRegistry : AccountNavigationCommand()

    object OnAccountCreated : AccountNavigationCommand()

    object OnSAMLCommandAccessTokenObtained : AccountNavigationCommand()
  }
}
