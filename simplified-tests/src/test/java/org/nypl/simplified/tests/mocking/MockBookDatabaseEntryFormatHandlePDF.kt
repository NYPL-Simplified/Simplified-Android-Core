package org.nypl.simplified.tests.mocking

import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.formats.api.StandardFormatNames
import java.io.File

class MockBookDatabaseEntryFormatHandlePDF(
  val bookID: BookID
) : BookDatabaseEntryFormatHandlePDF() {

  var bookData: String? = null
  private var bookFile: File? = null

  var formatField: BookFormat.BookFormatPDF =
    BookFormat.BookFormatPDF(
      drmInformation = BookDRMInformation.None,
      file = this.bookFile,
      lastReadLocation = null,
      contentType = StandardFormatNames.genericPDFFiles
    )

  var drmInformationHandleField: BookDRMInformationHandle =
    object : BookDRMInformationHandle.NoneHandle() {
      override val info: BookDRMInformation.None =
        BookDRMInformation.None
    }

  override val format: BookFormat.BookFormatPDF
    get() = this.formatField

  override fun copyInBook(file: File) {
    this.bookData = file.readText()
    this.bookFile = file
    this.formatField = this.formatField.copy(file = this.bookFile)
    check(this.formatField.isDownloaded)
  }

  override fun setLastReadLocation(pageNumber: Int?) {
    this.formatField = this.formatField.copy(lastReadLocation = pageNumber)
  }

  override val drmInformationHandle: BookDRMInformationHandle
    get() = this.drmInformationHandleField

  override fun setDRMKind(kind: BookDRMKind) {
    throw UnimplementedCodeException()
  }

  override fun deleteBookData() {
    this.bookData = null
    this.bookFile = null
    this.formatField = this.formatField.copy(file = this.bookFile)
  }
}
