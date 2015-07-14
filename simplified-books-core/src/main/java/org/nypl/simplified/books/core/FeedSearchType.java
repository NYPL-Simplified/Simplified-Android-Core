package org.nypl.simplified.books.core;

public interface FeedSearchType
{
  /**
   * Match the type of search.
   *
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchSearch(
    FeedSearchMatcherType<A, E> m)
    throws E;
}
