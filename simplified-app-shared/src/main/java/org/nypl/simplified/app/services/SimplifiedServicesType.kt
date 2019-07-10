package org.nypl.simplified.app.services

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.squareup.picasso.Picasso
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.helpstack.HelpstackType
import org.nypl.simplified.app.reader.ReaderHTTPServerType
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType
import org.nypl.simplified.theme.ThemeValue

/**
 * The application services.
 */

interface SimplifiedServicesType {

  /**
   * @return The Adobe DRM executor, or `null` if DRM is not supported
   */

  val adobeExecutor: AdobeAdeptExecutorType?

  /**
   * @return The local image loader used for all images that aren't book covers
   */

  val imageLoader: Picasso

  /**
   * @return The HTTP client used for requests
   */

  val http: HTTPType

  /**
   * @return The registry of account providers
   */

  val accountProviderRegistry: AccountProviderRegistryType

  /**
   * @return The network connectivity interface
   */

  val networkConnectivity: NetworkConnectivityType

  /**
   * @return The interface for retrieving screen size information
   */

  val screenSize: ScreenSizeInformationType

  /**
   * @return The book cover loader and generator
   */

  val bookCovers: BookCoverProviderType

  /**
   * @return The profiles controller
   */

  val profilesController: ProfilesControllerType

  /**
   * @return The books controller
   */

  val booksController: BooksControllerType

  /**
   * @return The reader bookmark service
   */

  val readerBookmarkService: ReaderBookmarkServiceUsableType

  /**
   * @return The general background executor
   */

  val backgroundExecutor: ListeningScheduledExecutorService

  /**
   * @return The readable book registry
   */

  val bookRegistry: BookRegistryReadableType

  /**
   * @return The OPDS feed loader service
   */

  val feedLoader: FeedLoaderType

  /**
   * @return The HelpStack instance, or `null` if HelpStack is not supported
   */

  val helpStack: HelpstackType?

  /**
   * @return The EPUB loader
   */

  val readerEPUBLoader: ReaderReadiumEPUBLoaderType

  /**
   * @return The reader HTTP server
   */

  val readerHTTPServer: ReaderHTTPServerType

  /**
   * @return The analytics service
   */

  val analytics: AnalyticsType

  /**
   * @return The document store
   */

  val documentStore: DocumentStoreType

  /**
   * @return The current application theme
   */

  val currentTheme: ThemeValue
}