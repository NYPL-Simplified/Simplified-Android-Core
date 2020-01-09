package org.nypl.simplified.profiles.api

/**
 * A profile selection event.
 */

sealed class ProfileSelection : ProfileEvent() {

  /**
   * A profile is in the process of being selected. All services that are listening for this
   * event should do whatever internal state updates they need to ensure that everything is
   * consistent by the time the completion event is published.
   */

  data class ProfileSelectionInProgress(
    val id: ProfileID
  ) : ProfileSelection()

  /**
   * A profile has finished being selected.
   */

  data class ProfileSelectionCompleted(
    val id: ProfileID
  ) : ProfileSelection()
}
