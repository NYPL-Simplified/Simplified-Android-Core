package org.nypl.simplified.ui.catalog

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory

/**
 * A view model factory that can produce view models for each of the sections of the application
 * that view feeds.
 */

class CatalogFeedViewModelFactory(
  private val application: Application,
  private val services: ServiceDirectoryType,
  private val feedArguments: CatalogFeedArguments,
  private val borrowViewModel: CatalogBorrowViewModel,
  private val listener: FragmentListenerType<CatalogFeedEvent>
) : ViewModelProvider.Factory {

  private val logger =
    LoggerFactory.getLogger(CatalogFeedViewModelFactory::class.java)

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    this.logger.debug("requested creation of view model of type {}", modelClass)

    return when {
      modelClass.isAssignableFrom(CatalogFeedViewModel::class.java) -> {
        val feedLoader: FeedLoaderType =
          this.services.requireService(FeedLoaderType::class.java)
        val booksController: BooksControllerType =
          this.services.requireService(BooksControllerType::class.java)
        val profilesController: ProfilesControllerType =
          this.services.requireService(ProfilesControllerType::class.java)
        val bookRegistry =
          services.requireService(BookRegistryType::class.java)
        val buildConfig: BuildConfigurationServiceType =
          this.services.requireService(BuildConfigurationServiceType::class.java)
        val analytics: AnalyticsType =
          services.requireService(AnalyticsType::class.java)

        CatalogFeedViewModel(
          this.application.resources,
          profilesController,
          feedLoader,
          booksController,
          bookRegistry,
          buildConfig,
          analytics,
          this.borrowViewModel,
          this.feedArguments,
          this.listener
        ) as T
      }
      else ->
        throw IllegalArgumentException(
          "This view model factory (${this.javaClass}) cannot produce view models of type $modelClass"
        )
    }
  }
}
