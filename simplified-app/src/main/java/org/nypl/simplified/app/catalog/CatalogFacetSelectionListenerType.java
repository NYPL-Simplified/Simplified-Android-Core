package org.nypl.simplified.app.catalog;

import org.nypl.simplified.opds.core.OPDSFacet;

/**
 * The type of facet selection listeners.
 */

public interface CatalogFacetSelectionListenerType
{
  /**
   * The facet <tt>f</tt> was selected.
   */

  void onFacetSelected(
    final OPDSFacet f);
}
