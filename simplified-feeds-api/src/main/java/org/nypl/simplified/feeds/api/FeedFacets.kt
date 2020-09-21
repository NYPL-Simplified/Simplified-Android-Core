package org.nypl.simplified.feeds.api

import com.io7m.jfunctional.Option
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetOPDS.Companion.ENTRYPOINT_FACET_GROUP_TYPE

/**
 * Functions to process facets.
 */

object FeedFacets {

  /**
   * Find the list of facets that represent an "entry point" group.
   *
   * @param feed The feed
   * @return A list of facets, or nothing if no entry point group is available
   */

  @JvmStatic
  fun findEntryPointFacetGroupForFeed(feed: Feed): List<FeedFacet>? {
    return when (feed) {
      is Feed.FeedWithoutGroups ->
        findEntryPointFacetGroup(feed.facetsByGroup)
      is Feed.FeedWithGroups ->
        findEntryPointFacetGroup(feed.facetsByGroup)
    }
  }

  /**
   * Find the list of facets that represent an "entry point" group.
   *
   * @param groups The facets by group
   * @return A list of facets, or nothing if no entry point group is available
   */

  @JvmStatic
  fun findEntryPointFacetGroup(
    groups: Map<String, List<FeedFacet>>
  ): List<FeedFacet>? {
    for (groupName in groups.keys) {
      val facets = groups[groupName]!!
      if (!facets.isEmpty()) {
        val facet = facets.get(0)
        if (facetIsEntryPointTyped(facet)) {
          return facets
        }
      }
    }

    return null
  }

  /**
   * @return `true` if all of the facet groups are "entry point" type facets
   */

  @JvmStatic
  fun facetGroupsAreAllEntryPoints(facetGroups: Map<String, List<FeedFacet>>): Boolean {
    return facetGroups.all { entry -> facetGroupIsEntryPointTyped(entry.value) }
  }

  /**
   * @return `true` if the given facet group is "entry point" typed
   */

  @JvmStatic
  fun facetGroupIsEntryPointTyped(facets: List<FeedFacet>): Boolean {
    return facets.all { facet -> facetIsEntryPointTyped(facet) }
  }

  /**
   * @return `true` if the given facet is "entry point" typed
   */

  @JvmStatic
  fun facetIsEntryPointTyped(facet: FeedFacet): Boolean {
    return when (facet) {
      is FeedFacet.FeedFacetOPDS ->
        facet.opdsFacet.groupType == Option.some(ENTRYPOINT_FACET_GROUP_TYPE)
      is FeedFacet.FeedFacetPseudo ->
        false
    }
  }
}
