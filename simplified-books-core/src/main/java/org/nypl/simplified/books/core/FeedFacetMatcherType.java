package org.nypl.simplified.books.core;

public interface FeedFacetMatcherType<A, E extends Exception>
{
  A onFeedFacetOPDS(
    FeedFacetOPDS f)
    throws E;

  A onFeedFacetPseudo(
    FeedFacetPseudo f)
    throws E;
}
