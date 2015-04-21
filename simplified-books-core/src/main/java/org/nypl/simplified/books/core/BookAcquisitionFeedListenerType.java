package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;

/**
 * The type of listeners for generating acquisiton feeds.
 */

public interface BookAcquisitionFeedListenerType
{
  /**
   * Generating the feed failed.
   */

  void onBookAcquisitionFeedFailure(
    Throwable x);

  /**
   * Generating the feed succeeded.
   */

  void onBookAcquisitionFeedSuccess(
    OPDSAcquisitionFeed f);
}
