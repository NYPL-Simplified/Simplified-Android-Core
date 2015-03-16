package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

/**
 * The type of navigation lane view listeners.
 */

public interface CatalogNavigationLaneViewListenerType
{
  /**
   * The user selected a book.
   *
   * @param v
   *          The lane
   * @param e
   *          The selected book
   */

  void onSelectBook(
    final CatalogNavigationLaneView v,
    final OPDSAcquisitionFeedEntry e);

  /**
   * The user selected a feed.
   *
   * @param v
   *          The lane
   * @param e
   *          The selected feed
   */

  void onSelectFeed(
    final CatalogNavigationLaneView v,
    final OPDSNavigationFeedEntry feed);
}
