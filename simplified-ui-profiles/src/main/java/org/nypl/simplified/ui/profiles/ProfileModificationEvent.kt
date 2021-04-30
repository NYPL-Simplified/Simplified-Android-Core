package org.nypl.simplified.ui.profiles

sealed class ProfileModificationEvent {

  object Succeeded : ProfileModificationEvent()

  object Cancelled : ProfileModificationEvent()
}
