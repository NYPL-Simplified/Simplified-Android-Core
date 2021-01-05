package org.nypl.simplified.ui.accounts

/**
 * The status of a login button.
 */

sealed class AccountLoginButtonStatus {

  /**
   * The login button should be displayed as a "Log Out" button, should be enabled, and should
   * execute [onClick] when clicked.
   */

  data class AsLogoutButtonEnabled(
    val onClick: () -> Unit
  ) : AccountLoginButtonStatus()

  /**
   * The login button should be displayed as a "Log Out" button, and should be disabled.
   */

  object AsLogoutButtonDisabled : AccountLoginButtonStatus()

  /**
   * The login button should be displayed as a "Log In" button, should be enabled, and should
   * execute [onClick] when clicked.
   */

  data class AsLoginButtonEnabled(
    val onClick: () -> Unit
  ) : AccountLoginButtonStatus()

  /**
   * The login button should be displayed as a "Log In" button, and should be disabled.
   */

  object AsLoginButtonDisabled : AccountLoginButtonStatus()

  /**
   * The login button should be displayed as a "Cancel" button, should be enabled, and should
   * execute [onClick] when clicked.
   */

  data class AsCancelButtonEnabled(
    val onClick: () -> Unit
  ) : AccountLoginButtonStatus()

  /**
   * The login button should be displayed as a "Cancel" button, and should be disabled.
   */

  object AsCancelButtonDisabled : AccountLoginButtonStatus()
}
