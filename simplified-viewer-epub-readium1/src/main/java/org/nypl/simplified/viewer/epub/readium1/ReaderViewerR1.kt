package org.nypl.simplified.viewer.epub.readium1

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

class ReaderViewerR1 : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(ReaderViewerR1::class.java)

  override val name: String =
    "org.nypl.simplified.viewer.epub.readium1.ReaderViewerR1"

  override fun canSupport(
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatPDF,
      is BookFormat.BookFormatAudioBook ->
        false

      is BookFormat.BookFormatEPUB -> {
        when (format.drmInformation) {
          is BookDRMInformation.ACS ->
            true
          is BookDRMInformation.LCP,
          BookDRMInformation.None -> {
            val r2Enabled = preferences.flags["useExperimentalR2"] ?: false
            return if (r2Enabled) {
              this.logger.warn("useExperimentalR2 is enabled, so R1 is disabled for DRM-free books")
              false
            } else {
              this.logger.warn("useExperimentalR2 is disabled, so R1 is enabled for DRM-free books")
              true
            }
          }
        }
      }
    }
  }

  override fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    val formatEPUB = format as BookFormat.BookFormatEPUB
    ReaderActivity.startActivity(
      activity,
      book.id,
      formatEPUB.file,
      FeedEntry.FeedEntryOPDS(book.account, book.entry)
    )
  }
}
