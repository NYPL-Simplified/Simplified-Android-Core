package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSFacet;

import com.io7m.jnull.NullCheck;

public final class FeedFacetOPDS implements FeedFacetType
{
  private static final long serialVersionUID = 1L;
  private final OPDSFacet   facet;

  public FeedFacetOPDS(
    final OPDSFacet in_facet)
  {
    this.facet = NullCheck.notNull(in_facet);
  }

  @Override public String facetGetTitle()
  {
    return this.facet.getTitle();
  }

  @Override public boolean facetIsActive()
  {
    return this.facet.isActive();
  }

  public OPDSFacet getOPDSFacet()
  {
    return this.facet;
  }

  @Override public <A, E extends Exception> A matchFeedFacet(
    final FeedFacetMatcherType<A, E> m)
    throws E
  {
    return m.onFeedFacetOPDS(this);
  }
}
