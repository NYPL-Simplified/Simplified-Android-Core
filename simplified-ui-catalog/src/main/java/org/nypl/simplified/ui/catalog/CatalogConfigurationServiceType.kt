package org.nypl.simplified.ui.catalog

/**
 * A configuration service used to customize the behaviour of the catalog.
 */

interface CatalogConfigurationServiceType {

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
}