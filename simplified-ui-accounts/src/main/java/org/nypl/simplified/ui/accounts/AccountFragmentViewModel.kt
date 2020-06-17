package org.nypl.simplified.ui.accounts

import androidx.lifecycle.ViewModel

/**
 * A view model for storing state during login attempts.
 */

class AccountFragmentViewModel : ViewModel() {

  /**
   * Logging in was explicitly requested. This is tracked in order to allow for optionally
   * closing the account fragment on successful logins.
   */

  @Volatile
  var loginExplicitlyRequested: Boolean = false
}
