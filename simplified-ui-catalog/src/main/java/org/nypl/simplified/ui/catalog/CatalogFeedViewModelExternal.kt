package org.nypl.simplified.ui.catalog

import android.content.Context
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

/**
 * The view model used for the "Catalog" section. That is, the section of the app that views
 * external OPDS feeds over the network.
 */

class CatalogFeedViewModelExternal(
  context: Context,
  services: ServiceDirectoryType
) : CatalogFeedViewModelAbstract(context, services) {

  override fun initialFeedArguments(
    context: Context,
    profiles: ProfilesControllerType
  ): CatalogFeedArguments {
    val account = profiles.profileAccountCurrent()
    return CatalogFeedArguments.CatalogFeedArgumentsRemote(
      title = context.getString(R.string.feedTitleCatalog),
      feedURI = account.provider.catalogURI,
      isSearchResults = false
    )
  }
}
