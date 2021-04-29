package org.nypl.simplified.main

sealed class MainActivityNavigationCommand {

  data class ProfilesNavigationCommand(
    val command: org.nypl.simplified.ui.profiles.ProfilesNavigationCommand
  ) : MainActivityNavigationCommand()
}
