package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

public interface CatalogNavigationLaneViewListenerType
{
  void onSelectBook(
    final CatalogNavigationLaneView v,
    final OPDSAcquisitionFeedEntry e);

  void onSelectFeed(
    final CatalogNavigationLaneView v,
    final OPDSNavigationFeedEntry feed);
}
