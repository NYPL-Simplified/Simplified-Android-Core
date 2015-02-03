package org.nypl.simplified.opds.core;

/**
 * A function to match on values of OPDS feed types.
 */

public interface OPDSFeedMatcherType<A, E extends Exception>
{
  A acquisition(
    final OPDSAcquisitionFeed f)
    throws E;

  A navigation(
    final OPDSNavigationFeed f)
    throws E;
}
