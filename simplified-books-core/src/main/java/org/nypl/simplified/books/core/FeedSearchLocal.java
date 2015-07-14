package org.nypl.simplified.books.core;


public final class FeedSearchLocal implements FeedSearchType
{
  public FeedSearchLocal()
  {

  }

  @Override public <A, E extends Exception> A matchSearch(
    final FeedSearchMatcherType<A, E> m)
    throws E
  {
    return m.onFeedSearchLocal(this);
  }
}
