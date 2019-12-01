package org.nypl.simplified.profiles.api

/**
 * The type of profile preferences events.
 */

sealed class ProfileUpdated : ProfileEvent() {

  abstract val profileID: ProfileID

  data class Succeeded(
    override val profileID: ProfileID,
    val oldPreferences: ProfilePreferences,
    val newPreferences: ProfilePreferences,
    val oldDisplayName: String,
    val newDisplayName: String
  ) : ProfileUpdated()

  data class Failed(
    override val profileID: ProfileID,
    val exception: Exception
  ) : ProfileUpdated()

}

