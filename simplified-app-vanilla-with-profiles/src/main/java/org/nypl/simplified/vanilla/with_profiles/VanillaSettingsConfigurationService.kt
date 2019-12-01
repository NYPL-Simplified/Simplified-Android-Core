package org.nypl.simplified.vanilla.with_profiles

import org.nypl.simplified.ui.settings.SettingsConfigurationServiceType

/**
 * The settings configuration service for the Vanilla application.
 */

class VanillaSettingsConfigurationService : SettingsConfigurationServiceType {

  override val allowAccountsAccess: Boolean
    get() = false

}
