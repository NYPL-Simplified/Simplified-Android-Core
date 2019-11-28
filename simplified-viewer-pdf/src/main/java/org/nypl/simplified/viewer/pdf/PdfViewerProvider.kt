package org.nypl.simplified.viewer.pdf

import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory

class PdfViewerProvider : ViewerProviderType {

  private val logger =
    LoggerFactory.getLogger(PdfViewerProvider::class.java)

  override val name: String =
    "org.nypl.simplified.viewer.pdf.PdfViewerProvider"

  override fun canSupport(
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
}