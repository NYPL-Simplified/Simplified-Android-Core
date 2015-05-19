package org.nypl.simplified.books.core;

public interface FeedMatcherType<A, E extends Exception>
{
  A onFeedWithGroups(
    FeedWithGroups f)
    throws E;

  A onFeedWithoutGroups(
    FeedWithoutGroups f)
    throws E;
}
