package org.nypl.simplified.viewer.pdf

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

class PdfViewerProvider : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(PdfViewerProvider::class.java)

  override val name: String =
    "org.nypl.simplified.viewer.pdf.PdfViewerProvider"

  override fun canSupport(
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ): Boolean {
    return when (format) {
      is BookFormat.BookFormatEPUB,
      is BookFormat.BookFormatAudioBook -> {
        logger.debug("the PDF viewer can only open PDF files!")
        false
      }
      is BookFormat.BookFormatPDF -> {
        true
      }
    }
  }

  override fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    val formatPDF = format as BookFormat.BookFormatPDF
    PdfReaderActivity.startActivity(
      from = activity,
      parameters = PdfReaderParameters(
        accountId = book.account,
        documentTile = book.entry.title,
        pdfFile = formatPDF.file!!,
        id = book.id
      )
    )
  }
}
