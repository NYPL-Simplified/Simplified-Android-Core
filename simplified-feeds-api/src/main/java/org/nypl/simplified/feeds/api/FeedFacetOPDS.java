package org.nypl.simplified.feeds.api;

import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSFacet;

/**
 * A facet taken from an OPDS feed.
 */

public final class FeedFacetOPDS implements FeedFacetType
{
  public static final String ENTRYPOINT_FACET_GROUP_TYPE =
    "http://librarysimplified.org/terms/rel/entrypoint";

  private static final long serialVersionUID = 1L;
  private final OPDSFacet facet;

  /**
   * Construct an OPDS facet.
   *
   * @param in_facet The actual OPDS facet
   */

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

  /**
   * @return The actual OPDS facet
   */

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
