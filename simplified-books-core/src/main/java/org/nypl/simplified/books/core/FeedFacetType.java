package org.nypl.simplified.books.core;

import java.io.Serializable;

/**
 * The type of feed facets.
 */

public interface FeedFacetType extends Serializable
{
  /**
   * @return The facet title
   */

  String facetGetTitle();

  /**
   * @return {@code true} iff the facet is active
   */

  boolean facetIsActive();

  /**
   * Match the type of feed facet.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchFeedFacet(
    final FeedFacetMatcherType<A, E> m)
    throws E;
}
