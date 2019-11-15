package org.nypl.simplified.ui.settings

import org.nypl.simplified.accounts.api.AccountID

/**
 * Navigation functions for the settings screens.
 */

interface SettingsNavigationControllerType {

  /**
   * The settings screen wants to open the "about" section.
   */

  fun openSettingsAbout()

  /**
   * The settings screen wants to open an account configuration screen for the given account.
   */

  fun openSettingsAccount(id: AccountID)

  /**
   * The settings screen wants to open the list of accounts.
   */

  fun openSettingsAccounts()

  /**
   * The settings screen wants to open the "acknowledgements" section.
   */

  fun openSettingsAcknowledgements()

  /**
   * The settings screen wants to open the "EULA" section.
   */

  fun openSettingsEULA()

  /**
   * The settings screen wants to open the "FAQ" section.
   */

  fun openSettingsFaq()

  /**
   * The settings screen wants to open the "license" section.
   */

  fun openSettingsLicense()

  /**
   * The settings screen wants to open the version screen.
   */

  fun openSettingsVersion()

  /**
   * The settings screen wants to pop the current screen from the stack.
   */

  fun popBackStack()
}