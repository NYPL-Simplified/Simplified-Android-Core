package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

/**
 * The type of acquisition feed listeners.
 */

public interface CatalogAcquisitionFeedListenerType
{
  /**
   * The user selected a book.
   * 
   * @param v
   *          The cell
   * @param e
   *          The selected book
   */

  void onSelectBook(
    final CatalogAcquisitionCellView v,
    final OPDSAcquisitionFeedEntry e);
}
