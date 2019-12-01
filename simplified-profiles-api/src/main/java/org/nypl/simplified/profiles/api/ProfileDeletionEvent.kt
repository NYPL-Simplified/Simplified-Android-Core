package org.nypl.simplified.profiles.api

/**
 * The type of events raised on profile deletion.
 */

sealed class ProfileDeletionEvent : ProfileEvent() {

  /**
   * The ID of the profile.
   */

  abstract val profileID: ProfileID

  /**
   * A profile was deleted successfully.
   */

  data class ProfileDeletionSucceeded(
    override val profileID: ProfileID)
    : ProfileDeletionEvent()

  /**
   * A profile could not be deleted.
   */

  data class ProfileDeletionFailed(
    override val profileID: ProfileID,

    /**
     * The exception raised.
     */

    val exception: Exception)
    : ProfileDeletionEvent()

}
