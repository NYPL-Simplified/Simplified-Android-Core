package org.nypl.simplified.ui.catalog

import android.content.Context
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

/**
 * The view model used for the "Holds" section. That is, the section of the app that views
 * books the user has reserved but does not yet have on loan.
 */

class CatalogFeedViewModelHolds(
  context: Context,
  services: ServiceDirectoryType
) : CatalogFeedViewModelAbstract(context, services) {

  override fun initialFeedArguments(
    context: Context,
    profiles: ProfilesControllerType
  ): CatalogFeedArguments {
    return CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
      title = context.getString(R.string.feedTitleHolds),
      facetType = FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE,
      searchTerms = null,
      selection = FeedBooksSelection.BOOKS_FEED_HOLDS
    )
  }
}
