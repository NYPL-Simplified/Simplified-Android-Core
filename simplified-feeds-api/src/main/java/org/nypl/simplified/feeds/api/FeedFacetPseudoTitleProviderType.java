package org.nypl.simplified.feeds.api;

/**
 * A title provider for a pseudo-facet.
 *
 * This interface essentially exists to allow pseudo-facets to take their titles
 * from Android resources without this module actually having to have a hard
 * dependency on Android.
 */

public interface FeedFacetPseudoTitleProviderType
{
  /**
   * @param t A pseudo-facet
   *
   * @return A title for the given pseudo-facet
   */

  String getTitle(
    FeedFacet.FeedFacetPseudo.FacetType t);
}
