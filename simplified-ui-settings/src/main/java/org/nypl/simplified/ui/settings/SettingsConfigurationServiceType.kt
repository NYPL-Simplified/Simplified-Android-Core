package org.nypl.simplified.ui.settings

/**
 * Configuration values for the settings views.
 */

interface SettingsConfigurationServiceType {

  /**
   * If set to `true`, then users are allowed access to the accounts panel and
   * can add/remove accounts. If set to `false`, the accounts setting item is
   * removed.
   */

  val allowAccountsAccess: Boolean

  /**
   * If set to `true`, then users are allowed access to the accounts registry and
   * can therefore create new accounts from that registry. This is in contrast to
   * the [allowAccountsAccess] flag, which simply prevents access to the entire
   * settings UI.
   */

  val allowAccountsRegistryAccess: Boolean
}
