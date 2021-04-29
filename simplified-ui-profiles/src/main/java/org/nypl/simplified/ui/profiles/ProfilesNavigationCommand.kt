package org.nypl.simplified.ui.profiles

import org.nypl.simplified.profiles.api.ProfileID

sealed class ProfilesNavigationCommand {

  /**
   * Open the "main" part of the application (in other words, whatever lives beyond the profile
   * selection screen). Note that the application MUST have selected a profile before calling this;
   * not doing so will lead to undefined results.
   */

  object OpenMain : ProfilesNavigationCommand()

  /**
   * Open the profile modification screen for profile `id`.
   */

  object OpenProfileSelect : ProfilesNavigationCommand()

  /**
   * Open a profile creation screen.
   */

  class OpenProfileModify(val id: ProfileID) : ProfilesNavigationCommand()

  /**
   * Open the profile selection screen.
   */

  object OpenProfileCreate : ProfilesNavigationCommand()

  object OnProfileModificationSucceeded : ProfilesNavigationCommand()

  object OnProfileModificationCancelled : ProfilesNavigationCommand()
}
