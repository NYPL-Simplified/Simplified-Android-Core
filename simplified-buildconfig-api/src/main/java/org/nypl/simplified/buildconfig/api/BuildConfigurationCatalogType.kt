package org.nypl.simplified.buildconfig.api

/**
 * Configuration values related to the catalog.
 */

interface BuildConfigurationCatalogType {

  /**
   * @return `true` if book detail pages should display status messages
   */

  val showDebugBookDetailStatus: Boolean

  /**
   * Should the "settings" tab be shown?
   *
   * If set to `true`, the settings tab will be visible.
   * If set to `false`, the settings tab will not be visible.
   */

  val showSettingsTab: Boolean

  /**
   * Should the "holds" tab be shown?
   *
   * If set to `true`, the holds tab will be visible.
   * If set to `false`, the holds tab will not be visible.
   */

  val showHoldsTab: Boolean

  /**
   * Should books from _all_ accounts be shown in the Books views?
   */

  val showBooksFromAllAccounts: Boolean

  /**
   * Enable/disable returning books.
   */

  @Deprecated(message = "Intended to solve a temporary issue with Open eBooks")
  fun allowReturns(): Boolean = true
}
