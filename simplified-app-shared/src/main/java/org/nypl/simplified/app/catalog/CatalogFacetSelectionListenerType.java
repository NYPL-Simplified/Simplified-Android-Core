package org.nypl.simplified.app.catalog;

import org.nypl.simplified.feeds.api.FeedFacet;

/**
 * The type of facet selection listeners.
 */

public interface CatalogFacetSelectionListenerType
{
  /**
   * The facet {@code f} was selected.
   *
   * @param f The facet
   */

  void onFacetSelected(
    final FeedFacet f);
}
