package org.nypl.simplified.ui.catalog

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.slf4j.LoggerFactory

/**
 * A view model factory that can produce view models for book borrowing operations.
 */

class CatalogBorrowViewModelFactory(
  private val services: ServiceDirectoryType
) : ViewModelProvider.Factory {

  private val logger =
    LoggerFactory.getLogger(CatalogBorrowViewModelFactory::class.java)

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    this.logger.debug("requested creation of view model of type {}", modelClass)

    return when {
      modelClass.isAssignableFrom(CatalogBorrowViewModel::class.java) ->
        CatalogBorrowViewModel(this.services) as T
      else ->
        throw IllegalArgumentException(
          "This view model factory (${this.javaClass}) cannot produce view models of type $modelClass"
        )
    }
  }

  companion object {
    fun get(
      host: Fragment
    ): CatalogBorrowViewModel {
      return ViewModelProviders.of(
        host,
        CatalogBorrowViewModelFactory(Services.serviceDirectory())
      ).get(CatalogBorrowViewModel::class.java)
    }
  }
}
