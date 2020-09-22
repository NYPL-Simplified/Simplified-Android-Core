package org.nypl.simplified.buildconfig.api

/**
 * Configuration values related to general application metadata.
 */

interface BuildConfigurationMetadataType {

  /**
   * The VCS commit (such as a Git commit ID) that produced this build.
   */

  val vcsCommit: String

  /** The version of Simplified Core used in this build. */

  val simplifiedVersion: String

  /**
   * The email address to which to send error reports. On most devices, users will be
   * able to override this as the address is passed to the external Android
   * mail activity, and this typically allows for editing both the message and the
   * sender address.
   */

  val supportErrorReportEmailAddress: String

  /**
   * The subject text used in error reports.
   *
   * @see [supportErrorReportEmailAddress]
   */

  val supportErrorReportSubject: String
}
