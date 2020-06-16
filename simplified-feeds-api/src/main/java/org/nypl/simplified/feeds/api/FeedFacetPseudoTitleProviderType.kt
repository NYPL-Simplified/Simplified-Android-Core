package org.nypl.simplified.feeds.api

/**
 * A title provider for a pseudo-facet.
 *
 * This interface essentially exists to allow pseudo-facets to take their titles
 * from Android resources without this module actually having to have a hard
 * dependency on Android.
 */

interface FeedFacetPseudoTitleProviderType {
  val collection: String
  val collectionAll: String
  val sortBy: String
  val sortByAuthor: String
  val sortByTitle: String
}
