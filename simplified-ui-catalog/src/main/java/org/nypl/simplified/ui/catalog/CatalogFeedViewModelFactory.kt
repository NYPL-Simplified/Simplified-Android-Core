package org.nypl.simplified.ui.catalog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.ServiceDirectoryType
import org.slf4j.LoggerFactory

/**
 * A view model factory that can produce view models for each of the sections of the application
 * that view feeds.
 */

class CatalogFeedViewModelFactory(
  private val context: Context,
  private val services: ServiceDirectoryType,
  private val feedArguments: CatalogFeedArguments
) : ViewModelProvider.Factory {

  private val logger =
    LoggerFactory.getLogger(CatalogFeedViewModelFactory::class.java)

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    this.logger.debug("requested creation of view model of type {}", modelClass)

    return when {
      modelClass.isAssignableFrom(CatalogFeedViewModel::class.java) ->
        CatalogFeedViewModel(this.context, this.services, this.feedArguments) as T
      else ->
        throw IllegalArgumentException(
          "This view model factory (${this.javaClass}) cannot produce view models of type $modelClass"
        )
    }
  }
}
