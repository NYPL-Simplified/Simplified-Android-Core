package org.nypl.simplified.books.core;

/**
 * The type of feed matchers.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface FeedMatcherType<A, E extends Exception>
{
  /**
   * Match a feed.
   *
   * @param f The feed
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedWithGroups(
    FeedWithGroups f)
    throws E;

  /**
   * Match a feed.
   *
   * @param f The feed
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedWithoutGroups(
    FeedWithoutGroups f)
    throws E;
}
