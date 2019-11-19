package org.nypl.simplified.ui.catalog

class CatalogFragmentFeedExternal : CatalogFragmentFeedAbstract<CatalogFeedViewModelExternal>() {
  override val viewModelClass: Class<CatalogFeedViewModelExternal> =
    CatalogFeedViewModelExternal::class.java
}
