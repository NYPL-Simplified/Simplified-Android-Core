package org.nypl.simplified.books.feeds;

/**
 * The type of search document matchers.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface FeedSearchMatcherType<A, E extends Exception>
{
  /**
   * Match a type of search document.
   *
   * @param f The document
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedSearchOpen1_1(
    FeedSearchOpen1_1 f)
    throws E;

  /**
   * Match a type of search document.
   *
   * @param f The document
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedSearchLocal(
    FeedSearchLocal f)
    throws E;
}
