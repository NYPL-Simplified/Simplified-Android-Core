package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.util.Calendar;

/**
 * The type of OPDS feeds.
 */

public interface OPDSFeedType extends Serializable
{
  String getFeedID();

  String getFeedTitle();

  Calendar getFeedUpdated();

  /**
   * Match on the actual type of feed.
   *
   * @param m
   *          A matcher
   * @param <A>
   *          The type of values returned by the matcher
   * @param <E>
   *          The type of exceptions potentially raised by the matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher throws <code>E</code>
   */

  <A, E extends Exception> A matchFeedType(
    final OPDSFeedMatcherType<A, E> m)
    throws E;
}
