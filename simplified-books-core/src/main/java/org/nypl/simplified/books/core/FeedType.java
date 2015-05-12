package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSSearchLink;

import com.io7m.jfunctional.OptionType;

public interface FeedType
{
  /**
   * @return The unique identifier of the feed
   */

  String getFeedID();

  /**
   * @return The search URI for the feed
   */

  OptionType<OPDSSearchLink> getFeedSearchURI();

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
   * Match the type of feed.
   * 
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchFeed(
    FeedMatcherType<A, E> m)
    throws E;
}
