package org.nypl.simplified.books.feeds;

import com.io7m.jfunctional.OptionType;

import java.net.URI;
import java.util.Calendar;

/**
 * The type of mutable feeds.
 *
 * This provides an abstraction over parsed OPDS feeds, and locally generated
 * feeds from book databases. The user can match on the values using {@link
 * #matchFeed(FeedMatcherType)} to determine the real type of the feed.
 */

public interface FeedType
{
  /**
   * @return The unique identifier of the feed
   */

  String getFeedID();

  /**
   * @return The search URI for the feed
   */

  OptionType<FeedSearchType> getFeedSearch();

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
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchFeed(
    FeedMatcherType<A, E> m)
    throws E;

  /**
   * @return The number of entries in the feed
   */

  int size();
}
