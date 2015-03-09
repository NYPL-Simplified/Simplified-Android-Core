package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

public interface CatalogAcquisitionFeedListenerType
{
  void onSelectBook(
    final CatalogAcquisitionCellView v,
    final OPDSAcquisitionFeedEntry e);
}
