package org.nypl.simplified.buildconfig.api

/**
 * Configuration values related to OAuth.
 */

interface BuildConfigurationOAuthType {

  /**
   * The URI scheme used for OAuth callbacks.
   *
   * Note: This value _must_ match the scheme used in an intent filter in the manifest.
   */

  val oauthCallbackScheme: BuildConfigOAuthScheme
}
