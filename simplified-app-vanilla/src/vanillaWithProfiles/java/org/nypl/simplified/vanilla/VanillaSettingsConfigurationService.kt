package org.nypl.simplified.vanilla

import org.nypl.simplified.ui.settings.SettingsConfigurationServiceType

/**
 * The settings configuration service for the Vanilla application.
 */

class VanillaSettingsConfigurationService : SettingsConfigurationServiceType {
  override val allowAccountsRegistryAccess: Boolean = false
  override val allowAccountsAccess: Boolean = false
}
