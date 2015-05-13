package org.nypl.simplified.books.core;

public interface FeedMatcherType<A, E extends Exception>
{
  A onFeedWithBlocks(
    FeedWithBlocks f)
    throws E;

  A onFeedWithoutBlocks(
    FeedWithoutBlocks f)
    throws E;
}
