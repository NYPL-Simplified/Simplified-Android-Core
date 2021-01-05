package org.nypl.simplified.ui.accounts

import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * Navigation functions for the accounts screens.
 */

interface AccountNavigationControllerType : NavigationControllerType {

  /**
   * Open an account configuration screen for the given account.
   */

  fun openSettingsAccount(parameters: AccountFragmentParameters)

  /**
   * Open a SAML 2.0 login screen.
   */

  fun openSAML20Login(parameters: AccountSAML20FragmentParameters)

  /**
   * Open the error page.
   */

  fun openErrorPage(parameters: ErrorPageParameters)

  /**
   * Open the account registry.
   */

  fun openSettingsAccountRegistry()
}
