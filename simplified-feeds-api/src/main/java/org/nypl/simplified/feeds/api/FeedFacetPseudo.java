package org.nypl.simplified.feeds.api;

import com.io7m.jnull.NullCheck;

/**
 * A pseudo-facet.
 *
 * This is used to provide facets for locally generated feeds.
 */

public final class FeedFacetPseudo implements FeedFacetType
{
  private static final long serialVersionUID = 1L;
  private final boolean   active;
  private final String    title;
  private final FacetType type;

  /**
   * Construct a pseudo-facet.
   *
   * @param in_title  The facet title
   * @param in_active {@code true} if the facet is currently active
   * @param in_type   The type of facet
   */

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

  /**
   * @return The facet type
   */

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

  /**
   * The type of facets.
   */

  public enum FacetType
  {
    /**
     * Sort the feed in question by author.
     */

    SORT_BY_AUTHOR,

    /**
     * Sort the feed in question by book title.
     */

    SORT_BY_TITLE
  }
}
