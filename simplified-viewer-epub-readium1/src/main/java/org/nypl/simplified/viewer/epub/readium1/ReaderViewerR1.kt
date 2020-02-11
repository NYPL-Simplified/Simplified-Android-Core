package org.nypl.simplified.viewer.epub.readium1

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerProviderType

class ReaderViewerR1 : ViewerProviderType {

  override val name: String =
    "org.nypl.simplified.viewer.epub.readium1.ReaderViewerR1"

  override fun canSupport(
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatAudioBook -> false
      is BookFormat.BookFormatEPUB -> {
        format.adobeRights != null
      }
      is BookFormat.BookFormatPDF -> false
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
