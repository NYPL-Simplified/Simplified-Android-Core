package org.nypl.simplified.ui.settings

sealed class SettingsNavigationCommand {

  /**
   * The settings screen wants to open the "about" section.
   */

  object OpenSettingsAbout : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the list of accounts.
   */

  object OpenSettingsAccounts : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the "acknowledgements" section.
   */

  object OpenSettingsAcknowledgements : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the "EULA" section.
   */

  object OpenSettingsEULA : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the "FAQ" section.
   */

  object OpenSettingsFaq : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the "license" section.
   */

  object OpenSettingsLicense : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the version screen.
   */

  object OpenSettingsVersion : SettingsNavigationCommand()

  /**
   * The settings screen wants to open the custom OPDS creation form.
   */

  object OpenSettingsCustomOPDS : SettingsNavigationCommand()
}
