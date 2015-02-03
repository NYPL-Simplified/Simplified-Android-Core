package org.nypl.simplified.opds.core;

/**
 * The type of OPDS navigation feeds.
 */

public final class OPDSNavigationFeed implements OPDSFeedType
{
  @Override public <A, E extends Exception> A matchFeedType(
    final OPDSFeedMatcherType<A, E> m)
    throws E
  {
    return m.navigation(this);
  }
}
