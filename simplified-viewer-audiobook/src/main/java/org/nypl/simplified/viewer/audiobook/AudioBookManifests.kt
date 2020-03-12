package org.nypl.simplified.viewer.audiobook

import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.license_check.api.LicenseChecks
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_fulfill.api.ManifestFulfillmentStrategies
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicCredentials
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicParameters
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicType
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentErrorType
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentEvent
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.ServiceLoader

object AudioBookManifests {

  private val logger = LoggerFactory.getLogger(AudioBookManifests::class.java)

  /**
   * Attempt to synchronously download a manifest file. If the download fails, return the
   * error details.
   */

  fun downloadManifest(
    credentials: AccountAuthenticationCredentials?,
    manifestURI: URI,
    onManifestFulfillmentEvent: (ManifestFulfillmentEvent) -> Unit
  ): PlayerResult<ManifestFulfilled, ManifestFulfillmentErrorType> {
    this.logger.debug("downloadManifest")

    val strategies =
      ManifestFulfillmentStrategies.findStrategy(ManifestFulfillmentBasicType::class.java)
        ?: throw UnsupportedOperationException()

    val fulfillCredentials =
      if (credentials != null) {
        ManifestFulfillmentBasicCredentials(
          userName = credentials.barcode().value(),
          password = credentials.pin().value()
        )
      } else {
        null
      }

    val strategy =
      strategies.create(
        ManifestFulfillmentBasicParameters(
          uri = manifestURI,
          credentials = fulfillCredentials
        )
      )

    val fulfillSubscription =
      strategy.events.subscribe { event ->
        onManifestFulfillmentEvent(event)
      }

    try {
      return strategy.execute()
    } finally {
      fulfillSubscription.unsubscribe()
    }
  }

  /**
   * Attempt to save a manifest in the books database.
   */

  fun saveManifest(
    profiles: ProfilesControllerType,
    bookId: BookID,
    manifestURI: URI,
    manifest: ManifestFulfilled
  ) {
    val handle =
      profiles.profileAccountForBook(bookId)
        .bookDatabase
        .entry(bookId)
        .findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

    val contentType = manifest.contentType.fullType
    if (handle != null) {
      if (handle.formatDefinition.supportedContentTypes().contains(contentType)) {
        handle.copyInManifestAndURI(manifest.data, manifestURI)
      } else {
        this.logger.error(
          "Server delivered an unsupported content type: {}: ", contentType, IOException()
        )
      }
    } else {
      this.logger.error(
        "Bug: Book database entry has no audio book format handle", IllegalStateException()
      )
    }
  }

  /**
   * Attempt to parse a manifest file.
   */

  fun parseManifest(
    source: URI,
    data: ByteArray
  ): ParseResult<PlayerManifest> {
    this.logger.debug("parseManifest")

    val extensions =
      ServiceLoader.load(ManifestParserExtensionType::class.java)
        .toList()

    return ManifestParsers.parse(
      uri = source,
      streams = data,
      extensions = extensions
    )
  }

  /**
   * Attempt to perform any required license checks on the manifest.
   */

  fun checkManifest(
    manifest: PlayerManifest,
    onLicenseCheckEvent: (SingleLicenseCheckStatus) -> Unit
  ): Boolean {
    val singleChecks =
      ServiceLoader.load(SingleLicenseCheckProviderType::class.java)
        .toList()
    val check =
      LicenseChecks.createLicenseCheck(manifest, singleChecks)

    val checkSubscription =
      check.events.subscribe { event ->
        onLicenseCheckEvent(event)
      }

    try {
      val checkResult = check.execute()
      return checkResult.checkSucceeded()
    } finally {
      checkSubscription.unsubscribe()
    }
  }
}
