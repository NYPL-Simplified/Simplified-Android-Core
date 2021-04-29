package org.nypl.simplified.ui.onboarding

sealed class OnboardingNavigationCommand {

  data class AccountsNavigationCommand(
    val command: org.nypl.simplified.ui.accounts.AccountsNavigationCommand
  ) : OnboardingNavigationCommand()
}


