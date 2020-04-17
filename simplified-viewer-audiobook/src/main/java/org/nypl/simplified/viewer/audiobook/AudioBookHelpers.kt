package org.nypl.simplified.viewer.audiobook

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI

internal object AudioBookHelpers {

  private val logger =
    LoggerFactory.getLogger(AudioBookHelpers::class.java)

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

    val contentType = manifest.contentType
    if (handle == null) {
      this.logger.error(
        "Bug: Book database entry has no audio book format handle", IllegalStateException())
      return
    }

    if (!handle.formatDefinition.supports(contentType)) {
      this.logger.error(
        "Server delivered an unsupported content type: {}: ", contentType, IOException()
      )
      return
    }

    handle.copyInManifestAndURI(manifest.data, manifestURI)
  }
}
