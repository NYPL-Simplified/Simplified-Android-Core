package org.nypl.simplified.buildconfig.api

/**
 * A service to obtain access to various configuration values that are application-build-specific.
 */

interface BuildConfigurationServiceType {

  /**
   * The VCS commit (such as a Git commit ID) that produced this build.
   */

  val vcsCommit: String

  /**
   * The email address used to receive error and data migration reports.
   */

  val errorReportEmail: String

  /**
   * The URI scheme used for OAuth callbacks.
   *
   * Note: This value _must_ match the scheme used in an intent filter in the manifest.
   */

  val oauthCallbackScheme: BuildConfigOAuthScheme
}
