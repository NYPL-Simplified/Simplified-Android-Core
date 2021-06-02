package org.nypl.simplified.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
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
      modelClass.isAssignableFrom(CatalogBorrowViewModel::class.java) -> {
        val profilesController =
          services.requireService(ProfilesControllerType::class.java)
        val booksController: BooksControllerType =
          this.services.requireService(BooksControllerType::class.java)
        val bookRegistry =
          services.requireService(BookRegistryType::class.java)

        CatalogBorrowViewModel(profilesController, booksController, bookRegistry) as T
      }
      else ->
        throw IllegalArgumentException(
          "This view model factory (${this.javaClass}) cannot produce view models of type $modelClass"
        )
    }
  }
}
