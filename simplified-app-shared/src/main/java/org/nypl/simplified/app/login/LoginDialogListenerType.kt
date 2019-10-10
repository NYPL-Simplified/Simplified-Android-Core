package org.nypl.simplified.app.login

import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

/**
 * A listener interface that must be implemented by activities hosting login dialogs.
 */

interface LoginDialogListenerType {

  /**
   * The login dialog wants a reference to the profiles controller.
   */

  fun onLoginDialogWantsProfilesController(): ProfilesControllerType
}