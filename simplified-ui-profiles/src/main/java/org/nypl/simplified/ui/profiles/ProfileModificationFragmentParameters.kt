package org.nypl.simplified.ui.profiles

import org.nypl.simplified.profiles.api.ProfileID
import java.io.Serializable

data class ProfileModificationFragmentParameters(
  val profileID: ProfileID?
) : Serializable
