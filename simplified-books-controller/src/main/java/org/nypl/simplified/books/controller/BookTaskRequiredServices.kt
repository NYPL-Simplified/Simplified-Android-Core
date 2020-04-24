package org.nypl.simplified.books.controller

import android.content.ContentResolver
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.clock.ClockType
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType

class BookTaskRequiredServices(
  val adobeDRM: AdobeAdeptExecutorType?,
  val audioBookManifestStrategies: AudioBookManifestStrategiesType,
  val bookRegistry: BookRegistryType,
  val borrowStrings: BookBorrowStringResourcesType,
  val bundledContent: BundledContentResolverType,
  val clock: ClockType,
  val contentResolver: ContentResolver,
  val downloader: DownloaderType,
  val feedLoader: FeedLoaderType,
  val http: HTTPType,
  val profiles: ProfilesDatabaseType,
  val services: ServiceDirectoryType
) {
  companion object {
    fun createFromServices(
      contentResolver: ContentResolver,
      services: ServiceDirectoryType
    ): BookTaskRequiredServices {
      return BookTaskRequiredServices(
        adobeDRM = services.optionalService(AdobeAdeptExecutorType::class.java),
        audioBookManifestStrategies = services.requireService(AudioBookManifestStrategiesType::class.java),
        bundledContent = services.requireService(BundledContentResolverType::class.java),
        clock = services.requireService(ClockType::class.java),
        downloader = services.requireService(DownloaderType::class.java),
        feedLoader = services.requireService(FeedLoaderType::class.java),
        http = services.requireService(HTTPType::class.java),
        profiles = services.requireService(ProfilesDatabaseType::class.java),
        bookRegistry = services.requireService(BookRegistryType::class.java),
        borrowStrings = services.requireService(BookBorrowStringResourcesType::class.java),
        contentResolver = contentResolver,
        services = services
      )
    }
  }
}
