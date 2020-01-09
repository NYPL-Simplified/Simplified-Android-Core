package org.nypl.simplified.accounts.registry.api

/**
 * The status of the account provider registry.
 */

sealed class AccountProviderRegistryStatus {

  /**
   * The account provider registry is idle.
   */

  object Idle : AccountProviderRegistryStatus()

  /**
   * The account provider registry is currently refreshing.
   */

  object Refreshing : AccountProviderRegistryStatus()
}
