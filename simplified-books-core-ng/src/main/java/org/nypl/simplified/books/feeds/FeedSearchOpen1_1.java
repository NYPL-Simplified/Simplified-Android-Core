package org.nypl.simplified.books.feeds;

import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;

/**
 * An Open Search 1.1 document.
 */

public final class FeedSearchOpen1_1 implements FeedSearchType
{
  private final OPDSOpenSearch1_1 search;

  /**
   * Construct a search document.
   *
   * @param in_search The actual search document
   */

  public FeedSearchOpen1_1(
    final OPDSOpenSearch1_1 in_search)
  {
    this.search = NullCheck.notNull(in_search);
  }

  /**
   * @return The actual search document
   */

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
