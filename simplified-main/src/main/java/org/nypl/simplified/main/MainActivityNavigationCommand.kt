package org.nypl.simplified.main

import org.nypl.simplified.profiles.api.ProfileID

sealed class MainActivityNavigationCommand {

  sealed class ProfileCommand : MainActivityNavigationCommand() {

    object OpenMain : ProfileCommand()

    object OpenProfileSelect : ProfileCommand()

    class OpenProfileModify(val id: ProfileID) : ProfileCommand()

    object OpenProfileCreate : ProfileCommand()

    object OnProfileModificationSucceeded : ProfileCommand()

    object OnProfileModificationCancelled : ProfileCommand()
  }
}
