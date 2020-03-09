package org.nypl.simplified.profiles.api

/**
 * The type of profile preferences events.
 */

sealed class ProfileUpdated : ProfileEvent() {

  abstract val profileID: ProfileID

  data class Succeeded(
    override val profileID: ProfileID,
    val oldDescription: ProfileDescription,
    val newDescription: ProfileDescription
  ) : ProfileUpdated()

  data class Failed(
    override val profileID: ProfileID,
    val exception: Exception
  ) : ProfileUpdated()
}
