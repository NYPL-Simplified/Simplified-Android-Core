package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerProviderType

class ReaderViewerR2 : ViewerProviderType {

  override val name =
    "org.nypl.simplified.viewer.epub.readium2.ReaderViewerR2"

  override fun canSupport(
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatAudioBook -> false
      is BookFormat.BookFormatEPUB -> true
      is BookFormat.BookFormatPDF -> false
    }
  }

  override fun open(
    activity: Activity,
    book: Book,
    format: BookFormat
  ) {
    val bookId = book.id
    val file = (format as BookFormat.BookFormatEPUB).file!!
    val adobeRightsFile = format.adobeRightsFile
    val entry = FeedEntry.FeedEntryOPDS(book.account, book.entry)

    ReaderActivity.startActivity(
      accountId = book.account,
      bookId = bookId,
      context = activity,
      entry = entry,
      file = file,
      adobeRightsFile = adobeRightsFile
    )
  }
}
