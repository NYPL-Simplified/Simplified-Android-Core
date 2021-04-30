package org.nypl.simplified.ui.profiles

sealed class ProfileTabEvent {

  /**
   * The patron wants to switch profile.
   */

  object SwitchProfileSelected : ProfileTabEvent()
}
