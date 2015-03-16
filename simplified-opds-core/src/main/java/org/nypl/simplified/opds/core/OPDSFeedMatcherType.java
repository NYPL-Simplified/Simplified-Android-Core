package org.nypl.simplified.opds.core;

/**
 * A function to match on values of OPDS feed types.
 */

public interface OPDSFeedMatcherType<A, E extends Exception>
{
  A onAcquisitionFeed(
    final OPDSAcquisitionFeed f)
    throws E;

  A onNavigationFeed(
    final OPDSNavigationFeed f)
    throws E;
}
