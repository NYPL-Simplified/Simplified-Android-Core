package org.nypl.simplified.viewer.epub.readium1

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

class ReaderViewerR1 : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(ReaderViewerR1::class.java)

  override val name: String =
    "org.nypl.simplified.viewer.epub.readium1.ReaderViewerR1"

  override fun canSupport(
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatEPUB -> {
        true
      }
      is BookFormat.BookFormatAudioBook,
      is BookFormat.BookFormatPDF -> {
        this.logger.debug("R1 can only open EPUB files")
        false
      }
    }
  }

  override fun open(
    activity: Activity,
    book: Book,
    format: BookFormat
  ) {
    val formatEPUB = format as BookFormat.BookFormatEPUB
    ReaderActivity.startActivity(
      activity,
      book.id,
      formatEPUB.file,
      FeedEntry.FeedEntryOPDS(book.entry)
    )
  }
}
