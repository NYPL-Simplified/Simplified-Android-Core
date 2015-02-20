package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

public interface CatalogLaneViewListenerType
{
  void onSelectBook(
    final CatalogLaneView v,
    final OPDSAcquisitionFeedEntry e);

  void onSelectFeed(
    final CatalogLaneView v,
    final OPDSNavigationFeedEntry feed);
}
