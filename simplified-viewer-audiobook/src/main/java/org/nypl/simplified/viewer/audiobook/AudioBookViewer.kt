package org.nypl.simplified.viewer.audiobook

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.viewer.spi.ViewerPreferences
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
    preferences: ViewerPreferences,
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

  override fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    val formatAudio =
      format as BookFormat.BookFormatAudioBook
    val manifest =
      formatAudio.manifest!!

    val params =
      AudioBookPlayerParameters(
        accountID = book.account,
        bookID = book.id,
        manifestContentType = format.contentType.fullType,
        manifestFile = manifest.manifestFile,
        manifestURI = manifest.manifestURI,
        opdsEntry = book.entry,
        userAgent = HTTP.userAgent()
      )

    AudioBookPlayerActivity.startActivity(activity, params)
  }
}
