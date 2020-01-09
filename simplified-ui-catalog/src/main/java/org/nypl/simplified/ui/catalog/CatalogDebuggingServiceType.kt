package org.nypl.simplified.ui.catalog

/**
 * A debugging service used to enable/disable debug behaviour in the catalog.
 */

interface CatalogDebuggingServiceType {

  /**
   * @return `true` if book detail pages should display status messages
   */

  val showBookDetailStatus: Boolean
}
