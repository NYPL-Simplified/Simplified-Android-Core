package org.nypl.simplified.vanilla

import org.nypl.simplified.ui.catalog.CatalogConfigurationServiceType

/**
 * The catalog configuration service for the Vanilla application.
 */

class VanillaCatalogConfigurationService : CatalogConfigurationServiceType {
  override val showAllCollectionsInLocalFeeds: Boolean
    get() = true

  override val supportErrorReportEmailAddress: String
    get() = "simplyemigrationreports@nypl.org"

  override val supportErrorReportSubject: String
    get() = "[vanilla error report]"
}
