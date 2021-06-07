package org.nypl.simplified.buildconfig.api

import java.net.URI

/**
 * The Library Registry URIs.
 */

data class BuildConfigurationAccountsRegistryURIs(

  /**
   * The URI for the production Library Registry.
   */

  val registry: URI,

  /**
   * The URI for the QA Library Registry.
   */

  val registryQA: URI
)
