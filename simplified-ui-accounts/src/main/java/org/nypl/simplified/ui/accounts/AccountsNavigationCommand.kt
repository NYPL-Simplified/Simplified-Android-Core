package org.nypl.simplified.ui.accounts

import org.nypl.simplified.ui.accounts.saml20.AccountSAML20FragmentParameters
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * Navigation commands for the accounts screens.
 */

sealed class AccountsNavigationCommand {

  /**
   * Open an account configuration screen for the given account.
   */

  data class OpenSettingsAccount(
    val parameters: AccountFragmentParameters
  ) : AccountsNavigationCommand()

  /**
   * Open a SAML 2.0 login screen.
   */

  data class OpenSAML20Login(
    val parameters: AccountSAML20FragmentParameters
  ) : AccountsNavigationCommand()

  /**
   * Open the error page.
   */

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : AccountsNavigationCommand()

  /**
   * Open the account registry.
   */

  object OpenSettingsAccountRegistry
    : AccountsNavigationCommand()

  /**
   * Switch to whichever tab contains the catalog, forcing a reset of the tab.
   */

  object OpenCatalogAfterAuthentication
    : AccountsNavigationCommand()

  object OnAccountCreated
    : AccountsNavigationCommand()

  object OnSAMLEventAccessTokenObtained
    : AccountsNavigationCommand()
}
