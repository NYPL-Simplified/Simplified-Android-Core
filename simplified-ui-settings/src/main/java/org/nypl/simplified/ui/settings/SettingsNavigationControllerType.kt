package org.nypl.simplified.ui.settings

import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType

/**
 * Navigation functions for the settings screens.
 */

interface SettingsNavigationControllerType :
  NavigationControllerType,
  AccountNavigationControllerType {

  /**
   * The settings screen wants to open the "about" section.
   */

  fun openSettingsAbout()

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
   * The settings screen wants to open the custom OPDS creation form.
   */

  fun openSettingsCustomOPDS()
}
