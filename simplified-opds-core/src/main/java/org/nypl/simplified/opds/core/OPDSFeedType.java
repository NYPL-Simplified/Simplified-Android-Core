package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;

/**
 * The type of OPDS feeds.
 */

public interface OPDSFeedType extends Serializable
{
  /**
   * @return The unique identifier of the feed
   */

  String getFeedID();

  /**
   * @return The title of the feed
   */

  String getFeedTitle();

  /**
   * @return The last time the feed was updated
   */

  Calendar getFeedUpdated();

  /**
   * @return The URI of the feed
   */

  URI getFeedURI();

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
