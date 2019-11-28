package org.nypl.simplified.viewer.audiobook

import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

/**
 * An audio book viewer service.
 */

class AudioBookViewer : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(AudioBookViewer::class.java)

  override val name: String =
    "org.nypl.simplified.viewer.audiobook.AudioBookViewer"

  override fun canSupport(
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatEPUB,
      is BookFormat.BookFormatPDF -> {
        this.logger.debug("audio book viewer can only view audio books")
        false
      }
      is BookFormat.BookFormatAudioBook ->
        true
    }
  }
}