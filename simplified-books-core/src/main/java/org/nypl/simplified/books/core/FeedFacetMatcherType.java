package org.nypl.simplified.books.core;

/**
 * The type of feed facet matchers.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface FeedFacetMatcherType<A, E extends Exception>
{
  /**
   * Match a type of feed facet.
   *
   * @param f The facet
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedFacetOPDS(
    FeedFacetOPDS f)
    throws E;

  /**
   * Match a type of feed facet.
   *
   * @param f The facet
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedFacetPseudo(
    FeedFacetPseudo f)
    throws E;
}
