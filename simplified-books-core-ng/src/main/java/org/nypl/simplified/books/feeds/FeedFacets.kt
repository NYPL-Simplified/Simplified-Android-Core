package org.nypl.simplified.books.feeds

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.books.feeds.FeedFacetOPDS.ENTRYPOINT_FACET_GROUP_TYPE

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
  fun findEntryPointFacetGroupForFeed(feed: Feed): OptionType<List<FeedFacetType>> {
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
  private fun findEntryPointFacetGroup(
    groups: Map<String, List<FeedFacetType>>): OptionType<List<FeedFacetType>> {

    for (groupName in groups.keys) {
      val facets = groups[groupName]!!
      if (!facets.isEmpty()) {
        val facet = facets.get(0)
        if (facetIsEntryPointTyped(facet)) {
          return Option.some(facets)
        }
      }
    }

    return Option.none()
  }

  /**
   * @return `true` if all of the facet groups are "entry point" type facets
   */

  @JvmStatic
  fun facetGroupsAreAllEntryPoints(facetGroups: Map<String, List<FeedFacetType>>): Boolean {
    return facetGroups.all { entry -> facetGroupIsEntryPointTyped(entry.value) }
  }

  /**
   * @return `true` if the given facet group is "entry point" typed
   */

  @JvmStatic
  fun facetGroupIsEntryPointTyped(facets: List<FeedFacetType>): Boolean {
    return facets.all { facet -> facetIsEntryPointTyped(facet) }
  }

  /**
   * @return `true` if the given facet is "entry point" typed
   */

  @JvmStatic
  fun facetIsEntryPointTyped(facet: FeedFacetType): Boolean {
    return facet.matchFeedFacet(object : FeedFacetMatcherType<Boolean, UnreachableCodeException> {
      override fun onFeedFacetOPDS(facet: FeedFacetOPDS): Boolean {
        return facet.opdsFacet.groupType == Option.some(ENTRYPOINT_FACET_GROUP_TYPE)
      }

      override fun onFeedFacetPseudo(facet: FeedFacetPseudo): Boolean {
        return false
      }
    })
  }
}
