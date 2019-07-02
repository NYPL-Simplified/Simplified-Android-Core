package org.nypl.simplified.accounts.source.api

import java.net.URI

/**
 * The type of account provider description registry events.
 */

sealed class AccountProviderDescriptionRegistryEvent {

  /**
   * An account provider description was updated.
   */

  data class Updated(
    val id: URI)
    : AccountProviderDescriptionRegistryEvent()

  /**
   * An account provider source failed.
   */

  data class SourceFailed(
    val clazz: Class<*>,
    val exception: Exception)
    : AccountProviderDescriptionRegistryEvent()

}
