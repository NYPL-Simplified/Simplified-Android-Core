package org.nypl.simplified.ui.settings

sealed class SettingsMainEvent {

  /**
   * The settings screen wants to open the "about" screen.
   */

  object OpenAbout : SettingsMainEvent()

  /**
   * The settings screen wants to open the list of added accounts.
   */

  object OpenAccountList : SettingsMainEvent()

  /**
   * The settings screen wants to open the "acknowledgements" screen.
   */

  object OpenAcknowledgments : SettingsMainEvent()

  /**
   * The settings screen wants to open debug options.
   */

  object OpenDebugOptions : SettingsMainEvent()

  /**
   * The settings screen wants to open the "license" screen.
   */

  object OpenLicense : SettingsMainEvent()

  /**
   * The settings screen wants to open the "FAQ" screen.
   */

  object OpenFAQ : SettingsMainEvent()

  /**
   * The settings screen wants to open the "EULA" screen.
   */

  object OpenEULA : SettingsMainEvent()
}
