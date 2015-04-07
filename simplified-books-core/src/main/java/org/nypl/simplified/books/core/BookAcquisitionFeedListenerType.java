package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;

public interface BookAcquisitionFeedListenerType
{
  void onBookAcquisitionFeedSuccess(
    OPDSAcquisitionFeed f);

  void onBookAcquisitionFeedFailure(
    Throwable x);
}
