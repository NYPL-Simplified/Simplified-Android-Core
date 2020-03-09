package org.nypl.simplified.ui.catalog

/**
 * A configuration service used to customize the behaviour of the catalog.
 */

interface CatalogConfigurationServiceType {

  /**
   * Should the "settings" tab be shown?
   *
   * If set to `true`, the settings tab will be visible.
   * If set to `false`, the settings tab will not be visible.
   */

  val showSettingsTab: Boolean

  /**
   * Should all collections be shown in the local "My Books" and "Holds" feeds?
   *
   * If set to `true`, books from all collections will be shown in local feeds.
   * If set to `false`, only books from the currently selected account wil be shown.
   *
   * NYPL SimplyE builds have historically set this to `false`.
   * LFA builds have historically set this to `true`.
   * DRM-free builds have historically set this to `true`.
   */

  val showAllCollectionsInLocalFeeds: Boolean

  /**
   * Should the "holds" tab be shown?
   *
   * If set to `true`, the holds tab will be visible.
   * If set to `false`, the holds tab will not be visible.
   */

  val showHoldsTab: Boolean

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
