package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;

import com.io7m.jnull.NullCheck;

public final class FeedSearchOpen1_1 implements FeedSearchType
{
  private final OPDSOpenSearch1_1 search;

  public FeedSearchOpen1_1(
    final OPDSOpenSearch1_1 in_search)
  {
    this.search = NullCheck.notNull(in_search);
  }

  public OPDSOpenSearch1_1 getSearch()
  {
    return this.search;
  }

  @Override public <A, E extends Exception> A matchSearch(
    final FeedSearchMatcherType<A, E> m)
    throws E
  {
    return m.onFeedSearchOpen1_1(this);
  }
}
