package org.nypl.simplified.books.core;

/**
 * <p>The type of local searchers.</p>
 *
 * <p>This is actually just a marker type: When the application matches on a
 * value of type {@link FeedSearchType} and receives a {@code FeedSearchLocal},
 * then it knows to perform a search on a local feed.</p>
 */

public final class FeedSearchLocal implements FeedSearchType
{
  /**
   * Construct a local searcher.
   */

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
