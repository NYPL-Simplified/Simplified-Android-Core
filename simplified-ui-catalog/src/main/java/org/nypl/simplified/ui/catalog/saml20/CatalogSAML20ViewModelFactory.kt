package org.nypl.simplified.ui.catalog.saml20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import java.io.File

/**
 * A factory for SAML 2.0 view state.
 */

class CatalogSAML20ViewModelFactory(
  private val services: ServiceDirectoryType,
  private val listener: FragmentListenerType<CatalogSAML20Event>,
  private val parameters: CatalogSAML20FragmentParameters,
  private val webViewDataDir: File
) : ViewModelProvider.Factory {

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == CatalogSAML20ViewModel::class.java) {
      val profilesController =
        services.requireService(ProfilesControllerType::class.java)
      val booksController =
        services.requireService(BooksControllerType::class.java)
      val bookRegistry =
        services.requireService(BookRegistryType::class.java)
      val buildConfig =
        services.requireService(BuildConfigurationServiceType::class.java)

      return CatalogSAML20ViewModel(
        profilesController = profilesController,
        booksController = booksController,
        bookRegistry = bookRegistry,
        buildConfig = buildConfig,
        listener = this.listener,
        parameters = this.parameters,
        webViewDataDir = this.webViewDataDir
      ) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
