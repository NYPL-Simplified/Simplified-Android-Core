package org.nypl.simplified.ui.catalog

class CatalogFragmentFeedBooks : CatalogFragmentFeedAbstract<CatalogFeedViewModelBooks>() {
  override val viewModelClass: Class<CatalogFeedViewModelBooks> =
    CatalogFeedViewModelBooks::class.java
}
