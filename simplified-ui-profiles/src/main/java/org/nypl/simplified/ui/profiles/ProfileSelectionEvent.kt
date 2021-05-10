package org.nypl.simplified.ui.profiles

import org.nypl.simplified.profiles.api.ProfileID

sealed class ProfileSelectionEvent {

  /**
   * A profile has been selected.
   */

  object ProfileSelected : ProfileSelectionEvent()

  /**
   * Open a profile creation screen.
   */

  object OpenProfileCreation : ProfileSelectionEvent()

  /**
   * Open the profile modification screen for profile `id`.
   */

  data class OpenProfileModification(
    val profile: ProfileID
  ) : ProfileSelectionEvent()
}
