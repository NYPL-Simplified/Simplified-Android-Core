package org.nypl.simplified.ui.catalog

class CatalogFragmentFeedHolds : CatalogFragmentFeedAbstract<CatalogFeedViewModelHolds>() {
  override val viewModelClass: Class<CatalogFeedViewModelHolds> =
    CatalogFeedViewModelHolds::class.java
}
