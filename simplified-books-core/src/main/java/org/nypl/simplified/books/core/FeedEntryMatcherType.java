package org.nypl.simplified.books.core;

public interface FeedEntryMatcherType<A, E extends Exception>
{
  A onFeedEntryOPDS(
    FeedEntryOPDS e)
    throws E;

  A onFeedEntryCorrupt(
    FeedEntryCorrupt e)
    throws E;
}
