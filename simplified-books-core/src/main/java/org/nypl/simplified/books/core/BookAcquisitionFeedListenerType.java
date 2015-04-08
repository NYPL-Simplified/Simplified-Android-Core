package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;

public interface BookAcquisitionFeedListenerType
{
  void onBookAcquisitionFeedFailure(
    Throwable x);

  void onBookAcquisitionFeedSuccess(
    OPDSAcquisitionFeed f);
}
