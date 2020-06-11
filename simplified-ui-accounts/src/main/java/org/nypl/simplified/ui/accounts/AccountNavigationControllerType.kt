package org.nypl.simplified.ui.accounts

import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * Navigation functions for the accounts screens.
 */

interface AccountNavigationControllerType : NavigationControllerType {

  /**
   * The settings screen wants to open an account configuration screen for the given account.
   */

  fun openSettingsAccount(parameters: AccountFragmentParameters)

  /**
   * The settings screen wants to open the error page.
   */

  fun <E : PresentableErrorType> openErrorPage(parameters: ErrorPageParameters<E>)

  /**
   * The settings screen wants to open the account registry.
   */

  fun openSettingsAccountRegistry()
}
