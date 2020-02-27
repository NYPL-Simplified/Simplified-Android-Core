package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerProviderType

class ReaderViewerR2 : ViewerProviderType {

  override val name =
    "org.nypl.simplified.viewer.epub.readium2.ReaderViewerR2"

  override fun canSupport(book: Book, format: BookFormat): Boolean {
    return when (format) {
      is BookFormat.BookFormatAudioBook -> false
      is BookFormat.BookFormatEPUB -> {
        format.adobeRights == null
      }
      is BookFormat.BookFormatPDF -> false
    }
  }

  override fun open(activity: Activity, book: Book, format: BookFormat) {
    val bookId = book.id
    val file = (format as BookFormat.BookFormatEPUB).file!!
    val entry = FeedEntry.FeedEntryOPDS(book.entry)

    ReaderActivity.startActivity(activity, bookId, file, entry)
  }
}
