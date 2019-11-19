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
  val context: Context,
  val services: ServiceDirectoryType
) : ViewModelProvider.Factory {

  private val logger =
    LoggerFactory.getLogger(CatalogFeedViewModelFactory::class.java)

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    this.logger.debug("requested creation of view model of type {}", modelClass)

    return when {
      modelClass.isAssignableFrom(CatalogFeedViewModelBooks::class.java) ->
        CatalogFeedViewModelBooks(this.context, this.services) as T
      modelClass.isAssignableFrom(CatalogFeedViewModelHolds::class.java) ->
        CatalogFeedViewModelHolds(this.context, this.services) as T
      modelClass.isAssignableFrom(CatalogFeedViewModelExternal::class.java) ->
        CatalogFeedViewModelExternal(this.context, this.services) as T
      else ->
        throw IllegalArgumentException(
          "This view model factory (${this.javaClass}) cannot produce view models of type $modelClass")
    }
  }
}