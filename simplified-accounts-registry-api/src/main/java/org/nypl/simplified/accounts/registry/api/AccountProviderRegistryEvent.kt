package org.nypl.simplified.accounts.registry.api

import java.net.URI

/**
 * The type of account provider description registry events.
 */

sealed class AccountProviderRegistryEvent {

  /**
   * The status of the registry changed.
   */

  object StatusChanged: AccountProviderRegistryEvent()

  /**
   * An account provider was updated.
   */

  data class Updated(
    val id: URI)
    : AccountProviderRegistryEvent()

  /**
   * An account provider source failed.
   */

  data class SourceFailed(
    val clazz: Class<*>,
    val exception: Exception)
    : AccountProviderRegistryEvent()

}
