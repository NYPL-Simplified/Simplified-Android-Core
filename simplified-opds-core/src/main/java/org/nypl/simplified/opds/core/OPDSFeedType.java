package org.nypl.simplified.opds.core;

/**
 * The type of OPDS feeds.
 */

public interface OPDSFeedType
{
  <A, E extends Exception> A matchFeedType(
    final OPDSFeedMatcherType<A, E> m)
    throws E;
}
