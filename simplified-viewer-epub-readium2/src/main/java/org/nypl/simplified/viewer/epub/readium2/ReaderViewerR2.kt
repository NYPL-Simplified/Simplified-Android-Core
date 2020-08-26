package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

class ReaderViewerR2 : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(ReaderViewerR2::class.java)

  override val name =
    "org.nypl.simplified.viewer.epub.readium2.ReaderViewerR2"

  override fun canSupport(
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ): Boolean {
    val enabled = preferences.flags["useExperimentalR2"] ?: false
    if (!enabled) {
      this.logger.warn("useExperimentalR2 is either false, or not set, so R2 is disabled")
      return false
    }

    return when (format) {
      is BookFormat.BookFormatPDF,
      is BookFormat.BookFormatAudioBook ->
        false
      is BookFormat.BookFormatEPUB ->
        when (format.drmInformation) {
          is BookDRMInformation.ACS -> false
          is BookDRMInformation.LCP -> true
          BookDRMInformation.None -> true
        }
    }
  }

  override fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    val bookId = book.id
    val file = (format as BookFormat.BookFormatEPUB).file!!
    val entry = FeedEntry.FeedEntryOPDS(book.account, book.entry)

    ReaderActivity.startActivity(
      accountId = book.account,
      bookId = bookId,
      context = activity,
      entry = entry,
      file = file
    )
  }
}
