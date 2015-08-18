package org.nypl.simplified.books.core;

/**
 * The type of feed entry matchers.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface FeedEntryMatcherType<A, E extends Exception>
{
  /**
   * Match a type of feed entry.
   *
   * @param e The entry
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedEntryOPDS(
    FeedEntryOPDS e)
    throws E;

  /**
   * Match a type of feed entry.
   *
   * @param e The entry
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedEntryCorrupt(
    FeedEntryCorrupt e)
    throws E;
}
