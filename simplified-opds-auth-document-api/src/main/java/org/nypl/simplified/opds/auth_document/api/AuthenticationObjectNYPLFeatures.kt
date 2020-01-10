package org.nypl.simplified.opds.auth_document.api

/**
 * The NYPL's "features" extension to OPDS Authentication.
 *
 * @see "https://github.com/NYPL-Simplified/Simplified/wiki/Authentication-For-OPDS-Extensions#feature-flags"
 */

data class AuthenticationObjectNYPLFeatures(

  /**
   * The enabled feature flags.
   */

  val enabled: Set<String>,

  /**
   * The disabled feature flags.
   */

  val disabled: Set<String>
)
