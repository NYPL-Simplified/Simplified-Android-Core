package org.nypl.simplified.opds.core;

/**
 * The type of OPDS acquisition feeds.
 */

public final class OPDSAcquisitionFeed implements OPDSFeedType
{
  @Override public <A, E extends Exception> A matchFeedType(
    final OPDSFeedMatcherType<A, E> m)
    throws E
  {
    return m.acquisition(this);
  }
}
