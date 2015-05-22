package org.nypl.simplified.app.catalog;

import org.nypl.simplified.opds.core.OPDSFacet;

public interface CatalogFacetMenuListenerType
{
  void onFacetSelected(
    OPDSFacet facet);

  void onFacetSelectedNone();
}
