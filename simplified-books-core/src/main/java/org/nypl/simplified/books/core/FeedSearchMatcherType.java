package org.nypl.simplified.books.core;

public interface FeedSearchMatcherType<A, E extends Exception>
{
  A onFeedSearchOpen1_1(
    FeedSearchOpen1_1 f)
    throws E;

  A onFeedSearchLocal(
    FeedSearchLocal f)
    throws E;
}
