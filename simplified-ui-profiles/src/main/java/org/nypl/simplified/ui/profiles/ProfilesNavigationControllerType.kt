package org.nypl.simplified.ui.profiles

import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.profiles.api.ProfileID

/**
 * A navigation controller for profiles-related functionality.
 */

interface ProfilesNavigationControllerType : NavigationControllerType {

  /**
   * Open the "main" part of the application (in other words, whatever lives beyond the profile
   * selection screen). Note that the application MUST have selected a profile before calling this;
   * not doing so will lead to undefined results.
   */

  fun openMain()

  /**
   * Open the profile modification screen for profile `id`.
   */

  fun openProfileModify(id: ProfileID)

  /**
   * Open a profile creation screen.
   */

  fun openProfileCreate()

  /**
   * Open the profile selection screen.
   */

  fun openProfileSelect()
}
