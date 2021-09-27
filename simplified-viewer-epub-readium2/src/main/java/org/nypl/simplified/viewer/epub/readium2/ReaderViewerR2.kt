package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.nypl.simplified.viewer.spi.ViewerProviderType

class ReaderViewerR2 : ViewerProviderType {

  override val name =
    "org.nypl.simplified.viewer.epub.readium2.ReaderViewerR2"

  override fun canSupport(
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatPDF,
      is BookFormat.BookFormatAudioBook ->
        false
      is BookFormat.BookFormatEPUB ->
        true
    }
  }

  override fun canPotentiallySupportType(type: MIMEType): Boolean {
    return type.fullType == "application/epub+zip"
  }

  override fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    val bookId =
      book.id
    val file =
      (format as BookFormat.BookFormatEPUB).file!!
    val entry =
      FeedEntry.FeedEntryOPDS(book.account, book.entry)

    val parameters =
      Reader2ActivityParameters(
        accountId = book.account,
        drmInfo = format.drmInformation,
        bookId = bookId,
        file = file,
        entry = entry
      )

    Reader2Activity.startActivity(
      context = activity,
      parameters = parameters
    )
  }
}
