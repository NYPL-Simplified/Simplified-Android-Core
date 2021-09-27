package org.nypl.simplified.main

sealed class MainFragmentEvent {

  /**
   * The user explicitly asked for switching profile.
   */

  object SwitchProfileSelected : MainFragmentEvent()

  /**
   * A profile has been idle for a time and should be logged out.
   */

  object ProfileIdleTimedOut : MainFragmentEvent()
}
