package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

public final class FeedFacetPseudo implements FeedFacetType
{
  public static enum FacetType
  {
    SORT_BY_AUTHOR,
    SORT_BY_TITLE
  }

  private static final long serialVersionUID = 1L;
  private final boolean     active;
  private final String      title;
  private final FacetType        type;

  public FeedFacetPseudo(
    final String in_title,
    final boolean in_active,
    final FacetType in_type)
  {
    this.title = NullCheck.notNull(in_title);
    this.active = in_active;
    this.type = NullCheck.notNull(in_type);
  }

  @Override public String facetGetTitle()
  {
    return this.title;
  }

  @Override public boolean facetIsActive()
  {
    return this.active;
  }

  public FacetType getType()
  {
    return this.type;
  }

  @Override public <A, E extends Exception> A matchFeedFacet(
    final FeedFacetMatcherType<A, E> m)
    throws E
  {
    return m.onFeedFacetPseudo(this);
  }
}
