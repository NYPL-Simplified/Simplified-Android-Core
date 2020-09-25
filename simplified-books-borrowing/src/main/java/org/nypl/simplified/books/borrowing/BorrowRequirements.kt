package org.nypl.simplified.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.profiles.api.ProfileReadableType
import java.io.File

/**
 * The services required by borrow tasks.
 */

data class BorrowRequirements(
  val adobeExecutor: AdobeAdeptExecutorType?,
  val audioBookManifestStrategies: AudioBookManifestStrategiesType,
  val bookFormatSupport: BookFormatSupportType,
  val bookRegistry: BookRegistryType,
  val bundledContent: BundledContentResolverType,
  val cacheDirectory: File,
  val clock: () -> Instant,
  val contentResolver: ContentResolverType,
  val httpClient: LSHTTPClientType,
  val profile: ProfileReadableType,
  val services: ServiceDirectoryType,
  val subtasks: BorrowSubtaskDirectoryType,
  val temporaryDirectory: File
)
