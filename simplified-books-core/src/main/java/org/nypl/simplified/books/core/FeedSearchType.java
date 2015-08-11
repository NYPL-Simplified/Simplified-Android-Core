package org.nypl.simplified.books.core;

/**
 * The type of feed search documents.
 *
 * These can refer to search documents hosted on remote servers, or locally
 * implemented searchers that can operate on locally generated feeds.
 */

public interface FeedSearchType
{
  /**
   * Match the type of search.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchSearch(
    FeedSearchMatcherType<A, E> m)
    throws E;
}
